package dalmatinerdb.client

import org.scalatest._
import org.scalatest.Matchers
import org.scalatest.prop._
import scodec.Codec

final class FloatSpec extends FlatSpec
  with Matchers
  with GeneratorDrivenPropertyChecks {

  "Left-aligned float" should "encode" in {
    val floatValue = Value("3.6")
    val expectedBytes = // <<2,-13,36000000000000:48>>
      utils.toByteArray(Array(2, -12, 3, 70, 48, -72, -96, 0))
    val encoded = Codec.encode(floatValue)(Protocol.valueCodec).require.bytes.toArray
    encoded shouldEqual expectedBytes
  }

  "Right-aligned float" should "encode" in {
    val floatValue = Value("1.00000005")
    val expectedBytes = // <<2,-8,5:48>>.
      utils.toByteArray(Array(2, -12, 0, -24, -44, -91, -45, 79))
    val encoded = Codec.encode(floatValue)(Protocol.valueCodec).require.bytes.toArray
    expectedBytes shouldEqual encoded
  }

  "Negative float" should "encode" in {
    val floatValue = Value("-0.00000005")
    val expectedBytes = // <<2,-8,-5:48>>.
      utils.toByteArray(Array(2, -20, -5, 115, -40, -58, -80, 0))
    val encoded = Codec.encode(floatValue)(Protocol.valueCodec).require.bytes.toArray
    expectedBytes shouldEqual encoded
  }

  "Large float" should "encode" in {
    // 3.61e24 = 36 100 000 000 000 * 10^11
    val large = 3.61 * Math.pow(10, 24)
    val floatValue = Value(large.toString)
    val expectedBytes = // <<2,11,36100000000000:48>>
      utils.toByteArray(Array(2, 12, 3, 72, -124, -60, -124, 0))
    val encoded = Codec.encode(floatValue)(Protocol.valueCodec).require.bytes.toArray
    expectedBytes shouldEqual encoded
  }

  forAll { (value: Float) =>
    whenever (value > 0) {
      val floatValue = Value(value.toString)
      val encoded = Codec.encode(floatValue)(Protocol.valueCodec).require
      val decoded = Codec.decode(encoded)(Protocol.valueCodec).require.value
      decoded shouldEqual floatValue
    }
  }
}
