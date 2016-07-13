package dalmatinerdb.client

import org.scalatest._
import org.scalatest.prop._
import org.scalatest.Matchers
import scodec.Codec

final class IntSpec extends FlatSpec
  with Matchers
  with GeneratorDrivenPropertyChecks {

  "Positive integer" should "encode" in {
    val intValue = 36
    val expectedBytes = utils.toByteArray(Array(1,0,0,0,0,0,0,36))
    val encoded = Codec.encode(Value("36"))(Protocol.valueCodec).require.bytes.toArray
    encoded shouldEqual expectedBytes
  }

  forAll { (value: Int) =>
    whenever (value > 0) {
      val intValue = IntValue(value)
      val encoded = Codec.encode(intValue)(Protocol.intValue).require
      val decoded = Codec.decode(encoded)(Protocol.intValue).require.value
      decoded shouldEqual intValue
    }
  }
}
