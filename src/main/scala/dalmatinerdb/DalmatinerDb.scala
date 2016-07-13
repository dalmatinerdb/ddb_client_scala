package dalmatinerdb

import com.twitter.finagle.client.{DefaultPool, StackClient, StdStackClient, Transporter}
import com.twitter.finagle.netty3.Netty3Transporter
import com.twitter.finagle.transport.Transport
import com.twitter.finagle.{Name, Service, ServiceFactory, Stack}
import com.twitter.util.{Duration, Future}
import com.twitter.finagle.param.ProtocolLibrary

import dalmatinerdb.client.transport.DalmatinerDbClientPipelineFactory
import dalmatinerdb.client.{Request, Result, Startup}
import dalmatinerdb.client.transport.Packet

/** Rich builder methods for [[dalmatinerdb.DalmatinerDb]]
  *
  * Supplements a Finagle Client with convenient builder methods for
  * constructing a DalmatinerDb Client.
  */
trait DalmatinerDbRichClient { self: com.twitter.finagle.Client[Request, Result] =>

  /**
    * Creates a new `RichClient` connected to a logical destination described
    * by `dest` with the assigned `label`. The `label` is used to scope Client stats.
    */
  def newRichClient(dest: Name, label: String): dalmatinerdb.client.Client =
    client.Client(newClient(dest, label))

  /**
    * Creates a new `RichClient` connected to the logical destination described
    * by `dest`.
    */
  def newRichClient(dest: String): client.Client =
    client.Client(newClient(dest))
}

/** A Finagle Client that connects to DalmatinerDb Server(s).
  *
  * @example {{{
  * val client = DalmatinerDb.client
  *   .newRichClient("localhost:5555")
  * }}}
  *
  * To open a client bucket for writing:*
  * @example {{{
  * val client = DalmatinerDb.client
  *   .withBucket("bucket")
  *   .newRichClient("localhost:5555")
  * }}}
  */
object DalmatinerDb extends com.twitter.finagle.Client[Request, Result]
  with DalmatinerDbRichClient {

  /**
    * Implements a DalmatinerDb client in terms of a Finagle Stack Client.
    * The client inherits a wealth of features from Finagle including connection
    * pooling and load balancing.
    */
  case class Client(
    stack: Stack[ServiceFactory[Request, Result]] = StackClient.newStack,
    params: Stack.Params = StackClient.defaultParams
  ) extends StdStackClient[Request, Result, Client] with DalmatinerDbRichClient {

    protected type In = Packet
    protected type Out = Packet

    protected def copy1(
      stack: Stack[ServiceFactory[Request, Result]] = this.stack,
      params: Stack.Params = StackClient.defaultParams + DefaultPool.Param(
        low = 0, high = 1, bufferSize = 0,
        idleTime = Duration.Top,
        maxWaiters = Int.MaxValue) + ProtocolLibrary("ddb")): Client = copy(stack, params)

    protected def newTransporter = Netty3Transporter[Packet, Packet](
      new DalmatinerDbClientPipelineFactory(Startup(params)), params)

    protected def newDispatcher(transport: Transport[Packet, Packet]): Service[Request, Result] = {
      new dalmatinerdb.client.ClientDispatcher(transport, Startup(params))
    }

    /**
      * The name of the bucket where writes will be streamed
      */
    def withBucket(bucket: String): Client =
      configured(Startup.BucketName(Some(bucket)))

    /**
     * The time before writes are flushed on the server
     */
    def withDelay(delay: Int): Client =
      configured(Startup.Delay(Some(delay)))

    // Java-friendly forwarders
    // See https://issues.scala-lang.org/browse/SI-8905
    override def configured[P](psp: (P, Stack.Param[P])): Client = super.configured(psp)
  }

  /** Used to construct a Client via the builder pattern.  */
  val client = Client()

  def newClient(dest: Name, label: String): ServiceFactory[Request, Result] =
    client.newClient(dest, label)

  def newService(dest: Name, label: String): Service[Request, Result] =
    client.newService(dest, label)
}
