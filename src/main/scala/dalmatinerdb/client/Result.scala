package dalmatinerdb.client

import com.twitter.concurrent.AsyncStream

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
case class QueryResult(values: AsyncStream[DataPoint]) extends Result
