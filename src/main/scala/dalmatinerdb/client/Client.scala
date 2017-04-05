package dalmatinerdb.client

import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

import com.twitter.finagle._
import com.twitter.finagle.ServiceFactory
import com.twitter.util.{Closable, Future, Time, Return, Throw}

object Client {
  /**
   * Creates a new Client based on a ServiceFactory.
   */
  def apply(factory: ServiceFactory[Request, Result]): Client =
    new StdClient(factory)
}

trait Client extends Closable {
  def query(bucket: String, metric: Seq[String], time: Long, count: Long): Future[Result]
  def query(bucket: String, metric: Seq[String], time: Long, count: Long, opts: ReadOptions): Future[Result]
  def write(metric: Seq[String], time: Long, value: Value): Future[Result]
  def flush(): Future[Result]
  def close(deadline: Time): Future[Unit]
}

final class StdClient(val factory: ServiceFactory[Request, Result]) extends Client {
  private[this] val service = factory.toService

  def query(sql: String): Future[Result] =
    throw new NotImplementedError("Use query(bucket, metric, time, count) form")

  def query(bucket: String, metric: Seq[String], time: Long, count: Long): Future[Result] =
    withService { svc =>
      svc(Query(bucket, Metric(metric.toList), time, count))
    }

  def query(bucket: String, metric: Seq[String], time: Long, count: Long, opts: ReadOptions): Future[Result] =
    withService { svc =>
      svc(Query(bucket, Metric(metric.toList), time, count, opts))
    }

  def write(metric: Seq[String], time: Long, value: Value): Future[Result] =
    service(Write(Metric(metric.toList), time, value))

  def flush(): Future[Result] =
    service(Flush)

  def close(deadline: Time): Future[Unit] = factory.close(deadline)

  private def withService(f: Service[Request, Result] => Future[Result]) =
    factory() flatMap { svc =>
      f(svc).transform {
        case Return(r) =>
          svc.close()
          Future.value(r)
        case Throw(e) =>
          svc.close()
          Future.exception(e)
      }
    }
}
