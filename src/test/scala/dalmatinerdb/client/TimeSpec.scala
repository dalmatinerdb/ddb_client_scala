package dalmatinerdb.client

import org.scalatest._
import org.scalatest.Matchers
import org.scalatest.prop._
import scodec.Codec

final class TimeSpec extends FlatSpec
  with Matchers
  with GeneratorDrivenPropertyChecks {

  "Timestamp" should "encode" in {
    val ts = 1461602272L
    val expectedBytes = utils.toByteArray(Array(0,0,0,0,87,30,71,224))
    val encoded = Codec.encode(ts)(Protocol.timestamp).require.bytes.toArray
    expectedBytes shouldEqual encoded
  }

  forAll { (timestamp: Long) =>
    whenever (timestamp > 0) {
      val encoded = Codec.encode(timestamp)(Protocol.timestamp).require
      val decoded = Codec.decode(encoded)(Protocol.timestamp).require.value
      decoded shouldEqual timestamp
    }
  }
}
