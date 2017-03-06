package dalmatinerdb.client

import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

import com.twitter.finagle._
import com.twitter.finagle.ServiceFactory
import com.twitter.util.{Closable, Future, Time}

object Client {
  /**
   * Creates a new Client based on a ServiceFactory.
   */
  def apply(factory: ServiceFactory[Request, Result]): Client =
    new StdClient(factory)
}

trait Client extends Closable {
  def query(bucket: String, metric: String, time: Long, count: Long): Future[Result]
  def query(bucket: String, metric: String, time: Long, count: Long, opts: ReadOptions): Future[Result]
  def write(metric: String, time: Long, value: Value): Future[Result]
  def flush(): Future[Result]
  def close(): Future[Unit]
}

final class StdClient(val factory: ServiceFactory[Request, Result]) extends Client {
  private[this] val service = factory.toService

  def query(sql: String): Future[Result] =
    throw new NotImplementedError("Use query(bucket, metric, time, count) form")

  def query(bucket: String, metricPath: String, time: Long, count: Long): Future[Result] =
    service(Query(bucket, new Metric(metricPath), time, count))

  def query(bucket: String, metricPath: String, time: Long, count: Long, opts: ReadOptions): Future[Result] =
    service(Query(bucket, new Metric(metricPath), time, count, opts))

  def write(metricPath: String, time: Long, value: Value): Future[Result] =
    service(Write(new Metric(metricPath), time, value))

  def flush(): Future[Result] =
    service(Flush)

  def close(deadline: Time): Future[Unit] = service.close(deadline)
}
