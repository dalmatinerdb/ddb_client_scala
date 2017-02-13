package dalmatinerdb.client.transport

import java.nio.ByteOrder
import java.nio.charset.Charset
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}

/** This source in this module has been modified slightly from the MySql and Postgres implementations found in Twitter's
  * finagle project.
  * Credit: https://github.com/twitter/finagle
  */
object Buffer {
  def apply(bytes: Array[Byte]): Buffer = new Buffer {
    val underlying = ChannelBuffers.wrappedBuffer(bytes)
  }

  def fromChannelBuffer(cb: ChannelBuffer): Buffer = {
    require(cb != null)
    require(cb.order == ByteOrder.BIG_ENDIAN, "Invalid ChannelBuffer ByteOrder")
    new Buffer { val underlying = cb }
  }
}

sealed trait Buffer {
  val underlying: ChannelBuffer
  def capacity: Int = underlying.capacity
}

trait BufferReader extends Buffer {

  /** Current reader offset in the buffer. */
  def offset: Int

  /**
   * Denotes if the buffer is readable upto the given width
   * based on the current offset.
   */
  def readable(width: Int): Boolean

  def readByte: Byte

  /**
   * Increases offset by n.
   */
  def skip(n: Int): Unit

  /**
   * Consumes the rest of the buffer and returns
   * it in a new Array[Byte].
   * @return Array[Byte] containing the rest of the buffer.
   */
  def takeRest(): Array[Byte] = take(capacity - offset)

  /**
   * Consumes n bytes in the buffer and
   * returns them in a new Array.
   * @return An Array[Byte] containing bytes from offset to offset+n
   */
  def take(n: Int): Array[Byte]

  /**
   * Returns the bytes from start to start+length
   * into a string using the given java.nio.charset.Charset.
   */
  def toString(start: Int, length: Int, charset: Charset): String
}

object BufferReader {

  def apply(buf: Buffer, offset: Int = 0): BufferReader = {
    require(offset >= 0, "Invalid reader offset")
    buf.underlying.readerIndex(offset)
    new Netty3BufferReader(buf.underlying)
  }

  def apply(bytes: Array[Byte]): BufferReader =
    apply(Buffer(bytes), 0)

  /**
   * BufferReader implementation backed by a Netty3 ChannelBuffer.
   */
  private[this] final class Netty3BufferReader(val underlying: ChannelBuffer)
    extends BufferReader with Buffer {
    def offset: Int          = underlying.readerIndex

    def readable(width: Int) = underlying.readableBytes >= width

    def readByte: Byte = underlying.readByte()

    def skip(n: Int) = underlying.skipBytes(n)

    def take(n: Int) = {
      val res = new Array[Byte](n)
      underlying.readBytes(res)
      res
    }

    def toString(start: Int, length: Int, charset: Charset) =
      underlying.toString(start, length, charset)
  }
}

/**
  * Provides convenience methods for writing data into a dalmatiner packet,
  * in big endian order.
  * This source has been adapted from the examples in Twitter's finagle project:
  * https://github.com/twitter/finagle
  */
trait BufferWriter extends Buffer {

  /**
   * Current writer offset.
   */
  def offset: Int

  /**
   * Denotes if the buffer is writable upto the given width
   * based on the current offset.
   */
  def writable(width: Int): Boolean

  def writeByte(n: Int): BufferWriter

  def skip(n: Int): BufferWriter

  def toBytes: Array[Byte]
  /**
   * Fills the rest of the buffer with the given byte.
   * @param b Byte used to fill.
   */
  def fillRest(b: Byte) = fill(capacity - offset, b)

  /**
   * Fills the buffer from current offset to offset+n with b.
   * @param n width to fill
   * @param b Byte used to fill.
   */
  def fill(n: Int, b: Byte) = {
    (offset until offset + n) foreach { j => writeByte(b) }
    this
  }

  /**
   * Writes bytes onto the buffer.
   * @param bytes Array[Byte] to copy onto the buffer.
   */
   def writeBytes(bytes: Array[Byte]): BufferWriter
}

private[transport] object BufferWriter {

  def apply(buf: Buffer, offset: Int = 0): BufferWriter = {
    require(offset >= 0, "Inavlid writer offset.")
    buf.underlying.writerIndex(offset)
    new Netty3BufferWriter(buf.underlying)
  }

  def apply(bytes: Array[Byte]): BufferWriter =
    apply(Buffer(bytes), 0)

  /**
   * BufferWriter implementation backed by a Netty ChannelBuffer.
   */
  private[this] class Netty3BufferWriter(val underlying: ChannelBuffer)
    extends BufferWriter with Buffer {
    def offset = underlying.writerIndex
    def writable(width: Int = 1): Boolean = underlying.writableBytes >= width

    def writeByte(n: Int): BufferWriter = {
      underlying.writeByte(n)
      this
    }

    def skip(n: Int) = {
      underlying.writerIndex(offset + n)
      this
    }

    def writeBytes(bytes: Array[Byte]) = {
      underlying.writeBytes(bytes)
      this
    }

    def toBytes: Array[Byte] = {
      val bytes = new Array[Byte](underlying.writerIndex)
      underlying.getBytes(0, bytes)
      bytes
    }
  }
}
