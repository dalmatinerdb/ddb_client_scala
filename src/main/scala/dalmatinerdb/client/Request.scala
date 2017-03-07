package dalmatinerdb.client

import scala.util.Try

/**
  * Represents any message originating from the client to the server
  */
sealed trait Request extends Product with Serializable

/**
  * Forces the server to flush all in-memory data to disk - only applies in streaming mode
  */
final case object Flush extends Request

/**
  * Opens streaming mode write operations to a bucket with the specified delay between flushes
  * @param delay the time delay with which to buffer points in the memory of the server
  * @param bucket the name of the bucket for which the write operations occur
  */
final case class EnableStream(delay: Int, bucket: String) extends Request

/**
  * Payload containing the information necessary for a single write
  * @param timestamp the event time at which the value occurred
  * @param metric the metric path that the data point pertains to
  * @param value either a float, integer or missing value point (will be recorded as zero)
  */
final case class Write(timestamp: Long, metric: List[String], value: Value) extends Request

object Write {

  /** Factory function for instantiating a [[dalmatinerdb.client.Write]] */
  def apply(metric: Metric, timestamp: Long, value: Value): Write = {
    require(timestamp > 0)
    require(metric.parts.nonEmpty, "Metric path cannot be empty")
    Write(timestamp, metric.parts.toList, value)
  }

  /** Factory function for instantiating a [[dalmatinerdb.client.Write]] */
  def apply(metric: Metric, timestamp: String, value: String): Write = {
    require(value.nonEmpty, "Data point value cannot be empty")
    require(timestamp.nonEmpty, "Timestamp cannot be empty")
    apply(metric, timestamp.toLong, Value(value))
  }
}

/**
  * Queries the given metric for the specified number of data points.
  * A [[dalmatinerdb.client.QueryResult]] will be eventually returned.
  * @param bucket The name of the bucket where the metric is stored
  * @param metric The name of the metric
  * @param time The starting time for the window of points to be returned
  * @param count The number of points to return - note that the resultset
  *              may have lower cardinality due to missing values
  * @param opts Consistency parameters for a read operation
  */
final case class Query(bucket: String,
                       metric: List[String],
                       time: Long,
                       count: Long,
                       rr: Int,
                       quorum: Int) extends Request

object Query {

  val defaultReadOptions = ReadOptions(ReadRepairOption.Default, ReadQuorumOption.Default)

  /** Factory function for instantiating a [[dalmatinerdb.client.Query]] */
  def apply(bucket: String,
            metric: Metric,
            timestamp: Long,
            count: Long,
            opts: ReadOptions = defaultReadOptions): Query = {
    require(bucket.nonEmpty, "Bucket cannot be empty")
    require(metric.parts.nonEmpty, "Metric path cannot be empty")
    require(timestamp > 0, "Timestamp cannot be empty")
    Query(bucket, metric.parts, timestamp, count, opts.readRepair.value, opts.quorum.value)
  }
}

/**
  * Any value may be encoded as an [[dalmatinerdb.client.IntValue]], [[dalmatinerdb.client.FloatValue]],
  * or [[dalmatinerdb.client.EmptyValue]]
  */
sealed trait Value extends Product with Serializable

final case class EmptyValue(zero: Long = 0) extends Value

final case class IntValue(value: Long) extends Value

final case class FloatValue(exp: Int, coefficient: Long) extends Value {
  lazy val value =
    coefficient * Math.pow(10, exp)

  override def toString(): String = s"FloatValue($value)"
}

object FloatValue {
  /** Factory function for instantiating a [[dalmatinerdb.client.FloatValue]] from an Double */
  def apply(x: Double): FloatValue = {
    val coefficient_digits = 13
    val sign = if (x >= 0) 1 else -1
    val d = Math.abs(x)
    val exponent =
      if (d == 0) 1
      else (Math.ceil(Math.log10(d)) - coefficient_digits).toInt
    val coefficient = (d / Math.pow(10, exponent)).toLong * sign
    FloatValue(exponent, coefficient)
  }

  /** Factory function for instantiating a [[dalmatinerdb.client.FloatValue]] from an Integer */
  def apply(i: Int): FloatValue = FloatValue(0, i)

  /** Factory function for instantiating a [[dalmatinerdb.client.FloatValue]] from a String */
  def apply(s: String): Option[FloatValue] = {
    Try(s.toInt).toOption
      .map((i: Int) => FloatValue(i))
      .orElse(Some(FloatValue(s.toDouble)))
  }
}

object IntValue {
  /** Factory function for instantiating a [[dalmatinerdb.client.IntValue]] from a String */
  def apply(s: String): Option[IntValue] =
    Try(s.toInt)
      .toOption
      .map(x => IntValue(x))
}


object Value {
  /** Factory function for instantiating a [[dalmatinerdb.client.Value]] from a String, will
    * attempt to parse the string in the following order: boolean -> integer -> float
    * Note that booleans are represented by the integers 0 and 1. */
  def apply(value: String): Value = {
    require(value.nonEmpty, "Value cannot be empty")

    def parseBool(s: String): Option[Value] = {
      val TrueValue = 1
      val FalseValue = 0

      s.trim.toLowerCase match {
        case "true" => Some(IntValue(TrueValue))
        case "false" => Some(IntValue(FalseValue))
        case _ => None
      }
    }

    parseBool(value)
      .orElse(IntValue.apply(value))
      .orElse(FloatValue.apply(value))
      .get
  }
}
