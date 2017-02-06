package dalmatinerdb.client

import com.twitter.finagle.dispatch.GenSerialClientDispatcher
import com.twitter.finagle.transport.Transport
import com.twitter.finagle.{CancelledRequestException, WriteException}
import com.twitter.util._
import scodec.bits.BitVector

import dalmatinerdb.client.transport.{BufferReader, Packet}

/**
 * A ClientDispatcher that implements the DalmatinerDB client/server protocol.
 */
class ClientDispatcher(trans: Transport[Packet, Packet], startup: Startup)
  extends GenSerialClientDispatcher[Request, Result, Packet, Packet](trans) {

  import ClientDispatcher._

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
    trans.write(encodePacket(req)) rescue {
      wrapWriteException
    } before {
      val signal = new Promise[Unit]
      if (startup.isStreamMode) {
        signal.setDone()
        rep.updateIfEmpty(Return(Ok))
        signal
      } else {
        trans.read() flatMap { packet =>
          rep.become(decodePacket(packet, req, signal))
          signal
        }
      }
    }

  /**
    * Returns a Packet representing the encoded request, using
    * the protocol codecs.
    * @param req is the request to encode
    * @return A Packet containing the raw bytes
    */
  private[this] def encodePacket(req: Request) = req match {
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
   * Returns a Future[Result] representing the decoded
   * packet. Some packets represent the start of a longer
   * transmission. These packets are distinguished by
   * the command used to generate the transmission.
   *
   * @param packet The first packet in the result.
   * @param req Is the Request that initiated the response - we can make
    *            this assumption here because we are using a serial queue
   * @param signal A future used to signal completion. When this
   * future is satisfied, subsequent requests can be dispatched.
   */
  private[this] def decodePacket(
    packet: Packet,
    req: Request,
    signal: Promise[Unit]
  ): Future[Result] = {

    def inner(raw: Array[Byte]) = req match {
      case w: Write => Ok
      case Flush => Ok
      case e: EnableStream => Ok

      case Query(_bucket, _metric, time, count) =>
        val result: List[Value] =
          Protocol.queryResultDecoder.decode(BitVector(raw)).require.value

        val startTime = time
        val endTime = time + count
        val range = startTime to endTime

        val datapoints = range
          .zip(result)
          .collect {
            case (t, i: IntValue) => DataPoint(t, i.value.toDouble)
            case (t, f: FloatValue) => DataPoint(t, f.value)
          }
        QueryResult(datapoints.toList)
    }

    val res = Try {
      val bytes = BufferReader(packet.body).takeRest()
      inner(bytes)
    }

    signal.setDone()
    const(res)
  }

  /**
   * Wrap a Try[T] into a Future[T]. This is useful for
   * transforming decoded results into futures. Any Throw
   * is assumed to be a failure to decode and thus a synchronization
   * error (or corrupt data) between the client and server.
   */
  private def const[T](result: Try[T]): Future[T] =
    Future.const(result rescue { case exc => Throw(exc) })
}

object ClientDispatcher {
  private val cancelledRequestExc = new CancelledRequestException
  private val wrapWriteException: PartialFunction[Throwable, Future[Nothing]] = {
    case exc: Throwable => Future.exception(WriteException(exc))
  }
}
