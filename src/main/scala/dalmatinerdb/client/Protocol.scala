package dalmatinerdb.client

import java.nio.charset.{Charset, StandardCharsets}

import org.xerial.snappy.Snappy
import scodec._
import scodec.bits.BitVector

private[client] object MessageTypes {
  final val ListMetrics  = 1  // not supported yet
  final val Query        = 2
  final val ListBuckets  = 3  // not supported yet
  final val EnableStream = 4
  final val Write        = 5
  final val Flush        = 6
  final val Batch        = 10 // not supported yet
}
/**
  * Used by the [[ClientDispatcher]] when exchanging messages between the client and the server
  * Request    -> Byte Array
  * Byte Array -> Response
  */
private[client] object Protocol {
  import scodec.codecs._

  private val dataSize = 8 // in bytes
  private val charset: Charset = StandardCharsets.UTF_8

  private val bucket: Codec[String] =
    variableSizeBytes(uint8, string(charset)).withToString(s"bucket(${charset.displayName})")

  private val metricElement: Codec[String] =
    variableSizeBytes(uint8, string(charset)).withToString(s"metric part(${charset.displayName})")

  private val metric: Codec[List[String]] =
    variableSizeBytes(uint16, list(metricElement)).withToString(s"metric(${charset.displayName})")

  val int56: Codec[Long] = new IntCodec(56)
  val int48: Codec[Long] = new IntCodec(48)
  val flushCodec: Codec[Unit] = sentry(MessageTypes.Flush)
  val timestamp: Codec[Long] = int64

  val enableStream = {
    val inner = {
      ("sentry"          | sentry(MessageTypes.EnableStream)       ) ::
      ("delay"           | uint8                                   ) ::
      ("bucket"          | bucket                                  )
    }.dropUnits.as[EnableStream]

    variableSizeBytes(int32, inner).withToString(s"enable stream")
  }

  val intValue: Codec[IntValue] = int56.xmap(IntValue.apply, _.value)
  val emptyValue: Codec[EmptyValue] = int56.xmap(EmptyValue.apply, _.zero)

  val floatValue: Codec[FloatValue] = {
    ("exp"             | int8              ) ::
    ("coefficient"     | int48             )
  }.as[FloatValue]

  val valueCodec: Codec[Value] = {
    discriminated[Value].by(int8)
      .\ (0) { case e: EmptyValue => e } (emptyValue)
      .\ (1) { case i: IntValue => i } (intValue)
      .\ (2) { case f: FloatValue => f } (floatValue)
  }

  val write: Codec[Write] = {
    ("sentry"          | sentry(MessageTypes.Write)                ) ::
    ("time"            | timestamp                                 ) ::
    ("metric"          | metric                                    ) ::
    ("value"           | variableSizeBytes(int32, valueCodec)      )
  }.dropUnits.as[Write]

  val query: Codec[Query] = {
    ("sentry"          | sentry(MessageTypes.Query)                ) ::
    ("bucket"          | bucket                                    ) ::
    ("metric"          | metric                                    ) ::
    ("time"            | timestamp                                 ) ::
    ("count"           | uint32                                    )
  }.dropUnits.as[Query]

  val queryResultDecoder: Decoder[List[Value]] = new QueryResultDecoder

  private def sentry(i: Int): Codec[Unit] = uint8.unit(i)

  private class IntCodec(size: Short) extends Codec[Long] {
    require(size < 64)

    def sizeBound = SizeBound.exact(size)

    def encode(l: Long) = {
      val bv = BitVector.fromLong(l).drop(64 - size)
      Attempt.successful(bv)
    }

    def decode(b: BitVector) = {
      val (result, remaining) = b.splitAt(size)
      if (result.size != size)
        Attempt.failure(new Err.InsufficientBits(size, result.size))
      else Attempt.successful(DecodeResult(result.toLong(), remaining))
    }
  }

  private class QueryResultDecoder extends Decoder[List[Value]] {
    private val sizeCodec = int32

    private val snappy = new Codec[BitVector] {
      def sizeBound = SizeBound.unknown

      def encode(b: BitVector) =
        Attempt.successful(BitVector(Snappy.compress(b.bytes.toArray)))

      def decode(b: BitVector) = {
        lazy val raw = b.bytes.toArray
        val remaining = BitVector.empty

        if (!Snappy.isValidCompressedBuffer(raw))
          Attempt.successful(DecodeResult(b, remaining))
        else {
          val res = BitVector(Snappy.uncompress(raw))
          Attempt.successful(DecodeResult(res, remaining))
        }
      }
    }

    private def padding =
      discriminated[BitVector].by(int8)
        .\ (0) { case empty => empty } (snappy)
        .\ (1) { case unpadded => unpadded } (snappy)
        .\ (2) { case padded => padded } (snappy)

    def decode(buffer: BitVector) = {
      for {
        decompressed <- variableSizeBytes(int32, padding).decode(buffer)
        values <- list(valueCodec).decode(decompressed.value)
      } yield values
    }
  }
}
