package dalmatinerdb.client

/**
  * A metric is typically identified by a name consisting of parts separated by a '.'
  * E.g. 'base.cpu'
  * @param path the metric path that can be split into it's components using the 'path' function
  */
final class Metric(val path: String) extends AnyVal {
  /**
    * Separate the metric path into its constituents
    * @return A list of metric parts/elements
    */
  def parts: Seq[String] = {
    val sep = '.'
    if (path.contains(sep)) path.split(sep) else Array(path)
  }
}