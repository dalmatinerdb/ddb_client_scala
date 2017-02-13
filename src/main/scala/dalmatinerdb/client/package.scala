package dalmatinerdb

package object client {
  /**
   * All datapoints values are represented as a decimal point in time
   * @param time the timestamp of the recorded value
   * @param value
   */
  type DataPoint = (Long, Option[Double])
}
