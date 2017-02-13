package dalmatinerdb.client

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.{CancelledRequestException, WriteException}
import com.twitter.finagle.dispatch.GenSerialClientDispatcher
import com.twitter.finagle.transport.Transport
import com.twitter.io.Buf
import com.twitter.util._
import scodec.bits.BitVector

import dalmatinerdb.client.transport.{BufferReader, Packet}

/**
 * A ClientDispatcher that implements the DalmatinerDB client/server protocol.
 */
class ClientDispatcher(trans: Transport[Packet, Packet], startup: Startup)
  extends GenSerialClientDispatcher[Request, Result, Packet, Packet](trans) {

  import ClientDispatcher._

  private val eof = Array[Byte](0.toByte, 0.toByte, 0.toByte, 1.toByte, 0.toByte)

  // The assumption is that one dispatcher is used per connection, as this
  // could be a race condition otherwise
  private var streamEnabled = false

  /**
    * Enabling stream mode is performed once per connection prior to any
    * writes between the client and server.
    **/
  override def apply(req: Request): Future[Result] =
    if (startup.isStreamMode && !streamEnabled) {
      val delay = startup.delay.get
      val bucketName = startup.bucketName.get
      val message = EnableStream(delay, bucketName)
      val signal = new Promise[Result]
      dispatch(message, signal).flatMap { _ => streamEnabled = true; super.apply(req) }
    } else {
      super.apply(req)
    }

  protected def dispatch(req: Request, rep: Promise[Result]): Future[Unit] =
    trans.write(encode(req)) rescue {
      wrapWriteException
    } before {
      decode(req, rep)
    }

  /**
    * Encodes a request using the protocol codecs.
    * @param req is the request to encode
    * @return A Packet containing the raw bytes
    */
  private[this] def encode(req: Request) = req match {
    // TODO: bring the codecs into scope implicitly
    case e: EnableStream =>
      Packet(Protocol.enableStream.encode(e).require.toByteArray)
    case q: Query =>
      Packet(Protocol.query.encode(q).require.toByteArray)
    case w: Write =>
      Packet(Protocol.write.encode(w).require.toByteArray)
    case Flush =>
      Packet(Protocol.flushCodec.encode(()).require.toByteArray)
  }
  /**
   * Decodes a response using the protocol codecs. Some streamed responses may
   * span multiple frames and they are aggregated until an EOF signal is
   * encountered.
    * @param req is the request to encode
    * @param rep is the writable value promise
    * @return Complete the promise withe the actual value
    */
  private[this] def decode(req: Request, rep: Promise[Result]) = req match {
    case w: Write => ok(rep)
    case Flush => ok(rep)
    case e: EnableStream => ok(rep)
    case Query(_bucket, _metric, time, count) =>
      val signal = new Promise[Unit]
      signal.setDone()
      rep.setValue(QueryResult(datapoints))
      signal
  }

  private[this] def ok(rep: Promise[Result]): Future[Unit] = {
    val signal = new Promise[Unit]
    if (startup.isStreamMode) {
      signal.setDone()
      rep.updateIfEmpty(Return(Ok))
      signal
    } else {
      trans.read().flatMap { _ =>
        rep.updateIfEmpty(Return(Ok))
        signal
      }
    }
  }

  private[this] def datapoints(): AsyncStream[Value] =
    AsyncStream.fromFuture(trans.read()).flatMap { packet =>
      BufferReader(packet.body).takeRest() match {
        case raw if raw.deep == eof.deep => AsyncStream.empty
        case raw =>
          val decoded = Protocol.queryResultDecoder.decode(BitVector(raw)).require.value
          AsyncStream.fromSeq[Value](decoded) ++ datapoints()
      }
    }
}

object ClientDispatcher {
  private val cancelledRequestExc = new CancelledRequestException

  private val wrapWriteException: PartialFunction[Throwable, Future[Nothing]] = {
    case exc: Throwable => Future.exception(WriteException(exc))
  }
}
