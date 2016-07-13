package dalmatinerdb.client

package object utils {
  def toByteArray(int: Seq[Int]): Seq[Byte] =
    int.toArray.map(x => x.toByte)
}
