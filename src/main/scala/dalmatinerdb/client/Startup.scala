package dalmatinerdb.client

import com.twitter.finagle.Stack

/** Represents information required for streaming writes to DalmatinerDb */
private[dalmatinerdb] object Startup {

  /** The bucket where writes will be streamed
    * @constructor
    * @param name the name of the bucket where writes will be streamed
    */
  case class BucketName(name: Option[String])
  implicit object BucketNameParam extends Stack.Param[BucketName] {
    val default = new BucketName(None)
  }

  /** The number of seconds with which to cache writes in memory before they
   *  are flushed - caching is done on the server.
    * @constructor
    * @param value the number of seconds before a flush operation
    */
  case class Delay(value: Option[Int]) {

    require(value.isEmpty || value.get <= 255, "delay must be < 255")
    require(value.isEmpty || value.get > 0, "delay must be > 0")
  }

  implicit object DelayParam extends Stack.Param[Delay] {
    val default = new Delay(None)
  }

  /** Builds a valid [[dalmatinerdb.client.Startup]] from a [[com.twitter.finagle.Stack]]
    * @param params [[com.twitter.finagle.Stack.Params]]
    */
  def apply(params: Stack.Params): Startup = {
    val BucketName(bucket) = params[BucketName]
    val Delay(delay) = params[Delay]
    new Startup(bucket, delay)
  }
}

/** Contains all information for starting a [[dalmatinerdb.client.ClientDispatcher]]
  * @param bucketName the name of the bucket where writes will be streamed
  * @param delay the delay before writes are flushed to disk on the server
  */
private[dalmatinerdb] case class Startup(bucketName: Option[String], delay: Option[Int]) {
  /** Indicates if stream mode has been configured on this client */
  def isStreamMode: Boolean =
    bucketName.nonEmpty && delay.nonEmpty
}
