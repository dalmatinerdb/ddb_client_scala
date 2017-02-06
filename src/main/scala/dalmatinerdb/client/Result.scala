package dalmatinerdb.client

/**
  * All datapoints values are represented as a decimal point in time
  * @param time the timestamp of the recorded value
  * @param value
  */
case class DataPoint(time: Long, value: Double)

/**
  * Represents a message from the server to the client
  */
sealed trait Result extends Product with Serializable

/**
  * Signals a side-effect in response to a command, where no result is returned
  */
case object Ok extends Result

/**
  * Representation of the result of a GET operation - all missing values for the time range requested are removed
  * @param points are a list of points in time/value pairs
  */
case class QueryResult(points: List[DataPoint]) extends Result

/**
  * Internal representation of the result of a GET operation
  * @param points are a list of either floats, ints or missing values
  */
private[client] case class RawQueryResult(points: List[Value])
