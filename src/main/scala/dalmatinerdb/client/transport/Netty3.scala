package dalmatinerdb.client.transport

import com.twitter.util.NonFatal
import dalmatinerdb.client.Startup
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.frame.{FrameDecoder, LengthFieldPrepender}

private[transport] final class PacketFrameDecoder extends FrameDecoder {

  override def decode(ctx: ChannelHandlerContext, channel: Channel,
                      buffer: ChannelBuffer): Packet = {

    if(buffer.readableBytes < Packet.HeaderSize) return null

    buffer.markReaderIndex()
    val length = buffer.readInt

    if(buffer.readableBytes < length) {
      buffer.resetReaderIndex()
      return null
    }

    // include all the original data in the buffer
    buffer.resetReaderIndex()
    val body = new Array[Byte](length + Packet.HeaderSize)
    buffer.readBytes(body)
    Packet(BufferReader(body))
  }
}

private[transport] final class PacketWriter extends SimpleChannelDownstreamHandler {
  override def writeRequested(ctx: ChannelHandlerContext, evt: MessageEvent) =
    evt.getMessage match {
      case p: Packet =>
        try {
          val cb = p.toChannelBuffer
          Channels.write(ctx, evt.getFuture, cb, evt.getRemoteAddress)
        } catch {
          case NonFatal(e) =>
            val _ = evt.getFuture.setFailure(new ChannelException(e.getMessage))
        }

      case unknown =>
        val _ = evt.getFuture.setFailure(new ChannelException(
          "Unsupported request type %s".format(unknown.getClass.getName)))
    }
}

/**
  * A Netty3 pipeline that is responsible for framing network traffic
 */
private[dalmatinerdb] final class DalmatinerDbClientPipelineFactory(startup: Startup)
  extends ChannelPipelineFactory {

  val FrameLengthFieldLength = 4
  val maximumPayloadBytes = Int.MaxValue

  def getPipeline = {
    val pipeline = Channels.pipeline()
    pipeline.addLast("packetDecoder", new PacketFrameDecoder)

    // A Dalmatiner node manages packet control itself when in stream mode
    if (!startup.isStreamMode)
      pipeline.addLast("packetHeaderWriter", new LengthFieldPrepender(FrameLengthFieldLength))

    pipeline.addLast("packetWriter", new PacketWriter)
    pipeline
  }
}
