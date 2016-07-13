package dalmatinerdb.client

import org.scalacheck.Gen
import org.scalatest._
import org.scalatest.Matchers
import org.scalatest.prop._
import scodec.Codec

final class BucketSpec extends FlatSpec
  with Matchers
  with GeneratorDrivenPropertyChecks {

  "Streaming mode command" should "encode" in {
    val bucket = "haggar1"
    val delay = 2
    val streamCmd = EnableStream(delay, bucket)
    val expectedBytes = utils.toByteArray(
      Array(0, 0, 0, 10, 4, 2, 7, 104, 97, 103, 103, 97, 114, 49))
    val encoded = Codec.encode(streamCmd)(Protocol.enableStream).require.bytes.toArray
    encoded shouldEqual expectedBytes
  }

  val bucketGen = Gen.alphaStr.filter(_.nonEmpty)
  val delayGen = Gen.choose(1,255)

  forAll(delayGen, bucketGen) { (delay: Int, bucket: String) =>
    val streamMode = EnableStream(delay, bucket)
    val encoded = Codec.encode(streamMode)(Protocol.enableStream).require
    val decoded = Codec.decode(encoded)(Protocol.enableStream).require.value
    decoded shouldEqual streamMode
  }
}
