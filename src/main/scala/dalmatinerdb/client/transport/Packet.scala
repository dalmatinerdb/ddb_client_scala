package dalmatinerdb.client.transport

import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}

private[dalmatinerdb] case class Packet(body: Buffer) {
  def length: Int = body.underlying.capacity

  def toChannelBuffer: ChannelBuffer = {
    val buffer = BufferWriter(new Array[Byte](length))
    buffer.writeBytes(body.underlying.array)
    ChannelBuffers.wrappedBuffer(buffer.toBytes)
  }
}

private[dalmatinerdb] object Packet {
  val HeaderSize = 4

  def apply(bytes: Array[Byte]): Packet =
    Packet(Buffer(bytes))
}
