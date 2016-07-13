package dalmatinerdb.client

import org.scalacheck.Gen
import org.scalatest._
import org.scalatest.Matchers
import org.scalatest.prop._
import scodec.Codec

final class MetricSpec extends FlatSpec
  with Matchers
  with GeneratorDrivenPropertyChecks {

  "A data point write" should "encode" in {
    val path = "base.cpu"
    val timestamp = "1463731200"
    val value = "90"
    val writeCmd = Write(new Metric(path), timestamp, value)
    val expectedBytes = utils.toByteArray(
      Array(5, 0, 0, 0, 0, 87, 62, -60, 0, 0, 9, 4, 98, 97, 115, 101, 3, 99,
        112, 117, 0, 0, 0, 8, 1, 0, 0, 0, 0, 0, 0, 90))
    val encoded = Codec.encode(writeCmd)(Protocol.write).require.bytes.toArray
    encoded shouldEqual expectedBytes
  }

  val metricGen = Gen.alphaStr.filter(_.nonEmpty)
  val tsGen = Gen.choose(464365500L, 1464365500L)
  val valueGen = Gen.choose(1,255)

  forAll(metricGen, tsGen, valueGen) { (path: String, timestamp: Long, value: Int) =>
    val writeCmd = Write(new Metric(path), timestamp.toString, value.toString)
    val encoded = Codec.encode(writeCmd)(Protocol.write).require
    val decoded = Codec.decode(encoded)(Protocol.write).require.value
    decoded shouldEqual writeCmd
  }
}
