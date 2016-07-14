# DalmatinerDB Scala / Java Client

[DalmatinerDB](https://github.com/dalmatinerdb/dalmatinerdb) is a metric database written in pure Erlang. It takes advantage of some special properties of metrics to make some tradeoffs. The goal is to make a store for metric data (time, value of a metric) that is fast, has a low overhead, and is easy to query and manage.

This client allows Scala (or Java) libraries to make use of DalmatinerDB as a metric store.  Twitter's [Finagle](https://github.com/twitter/finagle) RPC library serves as a substrate and allows the client to and leverage some of its features including:

- Connection pooling
- Load balancing
- Timeouts
- Retries

## Building

The client is built on Scala 2.11.8. To build the client, run:

    ./build/sbt assembly
    
(You do not need to do this if you downloaded a pre-built package.)

## Network protocol

Details of the DalmatinerDB network protocol can be found on the [project's website](http://dalmatinerdb.readthedocs.io/en/latest/proto.html)

## Using the client
    
    import dalmatinerDb.DalmatinerDb
    import com.twitter.util.Await
    
    val client = DalmatinerDb.client.newRichClient("127.0.0.1:5555")

It is also possible to specify a cluster and allow the client to load balance between nodes:

    val client = DalmatinerDb.client.newRichClient("127.0.0.1:5555,127.0.0.2:5555,127.0.0.3:5555")

Once all operations have been completed, the client should be closed:

    client.close()
    
## Querying data

Queries are formulated by supplying an initial timestamp and a count of the number of points to be returned:

    val queryResult = client.query("bucket", "base.cpu", 1443189800, 1000)
    Await.result(queryResult)

The result will be a `QueryResult` type consisting of a resolution and a sequence of time/value `Datapoint` pairs:

    DataPoint(1443189847,32.0)
    DataPoint(1443189877,20.0)
    DataPoint(1443189907,50.0)
    ....
    DataPoint(1443190657,20.0)
    DataPoint(1443190687,27.0)
    DataPoint(1443190717,20.0)
    DataPoint(1443190747,37.0)
    DataPoint(1443190777,20.0)

Note that all results are asynchronous and blocking on these results (as above) is discouraged in production.

DalmatinerDB also has a expressive query language and query engine that accessible via REST queries - the SQL statement variant is not yet supported by this client.

## Writing Data

For efficiency reasons, the Dalmatiner TCP endpoint can only accept incoming data when in stream mode. Therefore, in order to write data points, a client connection needs to be created in stream mode as follows:

    val client = DalmatinerDb.client.withBucket("fd").withDelay(2).newRichClient("127.0.0.1:5555")
    val wres = client.write("base.test", 1468419405, 0.1D)

## TODO

- [ ] Improve error handling
- [ ] Some operations are not supported, such as listing buckets and listing metrics
- [ ] Support raw SQL queries and result set iterators
- [ ] Better use of streaming interfaces - at this time the packet decoder consumes a packet in its entirety

## Contributing

The client is in the very early stages and is a work in progress - any contributions are welcome.
