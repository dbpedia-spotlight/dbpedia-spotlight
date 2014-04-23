package org.dbpedia.spotlight.storage

import scala.concurrent.duration.Duration
import scala.concurrent._
import com.redis.{RedisClientPool, RedisClient}
import org.apache.commons.pool.PoolableObjectFactory
import org.apache.commons.pool.impl.StackObjectPool
import scala.collection.mutable
import scala.util.Random
import com.redis.serialization.Parse.Implicits._
import scala.reflect.runtime.universe._


case class RedisCountStore(numOps: Int, masterHost: String = "localhost", masterPort: Int = 6379, slaves: Array[(String, Int)] = Array[(String, Int)]())(implicit executionContext: ExecutionContext)
  extends CountStore {

  val writerPool = new RedisMultipleClientPool(Array((masterHost, masterPort)))
  val readerPool = {
    if (slaves.isEmpty)
      new RedisMultipleClientPool(Array((masterHost, masterPort)))
    else
      new RedisMultipleClientPool(slaves)
  }

  def get(k: String): Option[Int] = readerPool.withClient(c => c.get[Int](k))

  def multiGet(ks: List[String], client: RedisClient): Option[List[Option[Int]]] =
    client.send[Option[List[Option[Int]]]]("MGET", ks)(client.asList[Int])

  def multiGet(ks: scala.collection.Set[String]): scala.collection.Map[String, Option[Int]] = {
    val keyList = ks.toList

    val results = {
      if (numOps > ks.size)
        readerPool.withClient(c => multiGet(keyList, c)).getOrElse(List[Option[Int]]())
      else {
        keyList.grouped(numOps)
          .map(keys => Future {
          readerPool.withClient(c => multiGet(keys, c))
        })
          .flatMap(r => Await.result(r, Duration.Inf).getOrElse(List[Option[Int]]()))
      }
    }

    keyList.zip(results.toIterable).foldLeft(mutable.Map[String, Option[Int]]())((acc, kv) => {
      acc += kv; acc
    })
  }

  def merge(kv: (String, Int)): Int = {
    writerPool.withClient(c => c.incrby(kv._1, kv._2)).get.toInt
  }

  def multiMerge(kvs: scala.collection.Map[String, Int]) = {
    /*val currentKvs = multiGet(kvs.keySet)
    val result = kvs.map{
        case (key,incr) => (key,currentKvs(key).getOrElse(0)+incr)
    }

    multiPut(result)
    result */
    kvs.grouped(numOps).map(m =>
      Future {
        writerPool.withClient(c => multiMerge(m, c))
      }).toList
      .flatMap(f => Await.result(f, Duration.Inf)).toMap
  }

  def multiMerge(kvs: scala.collection.Map[String, Int], client: RedisClient): scala.collection.Map[String, Int] = {
    val values = client.pipeline(c => {
      kvs.foreach(kv => c.incrby(kv._1, kv._2))
    }).asInstanceOf[Option[List[Option[Long]]]].get.map(_.get.toInt)
    kvs.keysIterator.zip(values.iterator).toMap
  }

  def put(kv: (String, Int)): Boolean = {
    if (kv._2 == 0) {
      writerPool.withClient(c => c.del(kv._1))
      true
    }
    else writerPool.withClient[Boolean](c => c.set(kv._1, kv._2))
  }

  def multiPut(kvs: scala.collection.Map[String, Int]): Boolean = {
    if (numOps > kvs.size)
      writerPool.withClient(c => multiPut(kvs, c))
    else
      kvs.grouped(numOps).map(kvs1 => Future {
        writerPool.withClient(c => multiPut(kvs1, c))
      })
        .map(r => Await.result(r, Duration.Inf)).reduce(_ && _)
  }

  def multiPut(kvs: scala.collection.Map[String, Int], client: RedisClient): Boolean = client.mset(kvs.toSeq: _*)

  def close() = {
    writerPool.close
    readerPool.close
  }

  def clear() = writerPool.withClient(_.flushall)

}

object RedisCountStore extends CountStoreType {
  def fromConfig(conf: CountStoreConfiguration) = {
    val host = conf.getHost
    var port = conf.getPort
    if (port < 0) port = 6379
    val slaves = conf.getProperty("redis.slaves", "").split(",").withFilter(!_.isEmpty).map(s => {
      val Array(host, port) = s.split(":", 2)
      (host, port.toInt)
    })
    val numOps = conf.getProperty("num_ops", "5000").toInt

    new RedisCountStore(numOps, host, port, slaves)(ExecutionContext.global)
  }

  def main(args: Array[String]) {
    implicit val ec = ExecutionContext.global

    val s = new RedisCountStore(5000)

    def test(f: () => Unit) {
      val start = System.currentTimeMillis()
      f()
      println("Duration: " + (System.currentTimeMillis() - start))
    }

    s.get("test")

    println()

    s.writerPool.withClient(_.flushall)
    val kvs: Map[String, Int] = (0 until 500000).map(k => ("te1_" + k, 10)).toMap

    val ks: Set[String] = (0 until 500000).map(k => "te1_" + k).toSet

    val batchMerge: () => Unit = () => {
      s.multiMerge(kvs)
    }

    val batchGet: () => Unit = () => {
      s.multiGet(ks)
    }

    test(batchMerge)
    println()
    test(batchGet)
    println()
    test(batchMerge)
    println()
    test(batchGet)
    println()

    (0 until 50).map(k => s.get("te1_" + k)).foreach(res => println(res))

    s.writerPool.withClient(_.flushall)
    s.close
    System.exit(0)
  }
}

private[storage] class RedisMultipleClientFactory(val hostAndPorts: Array[(String, Int)], val database: Int = 0, val secret: Option[Any] = None)
  extends PoolableObjectFactory[RedisClient] {

  val queue = new mutable.SynchronizedQueue[(String, Int)]()
  queue.enqueue(Random.shuffle(hostAndPorts.toList): _*)

  // when we make an object it's already connected
  def makeObject = queue.synchronized {
    val (host, port) = queue.dequeue()
    queue.enqueue((host, port))
    new RedisClient(host, port, database, secret)
  }

  // quit & disconnect
  def destroyObject(rc: RedisClient): Unit = {
    rc.quit // need to quit for closing the connection
    rc.disconnect // need to disconnect for releasing sockets
  }

  // noop: we want to have it connected
  def passivateObject(rc: RedisClient): Unit = {}

  def validateObject(rc: RedisClient) = rc.connected == true

  // noop: it should be connected already
  def activateObject(rc: RedisClient): Unit = {}
}

private[storage] class RedisMultipleClientPool(val hostAndPorts: Array[(String, Int)], val maxIdle: Int = 8, val database: Int = 0, val secret: Option[Any] = None) {
  val pool = new StackObjectPool(new RedisMultipleClientFactory(hostAndPorts, database, secret), maxIdle)

  override def toString = hostAndPorts.map(p => p._1 + ":" + p._2).mkString(",")

  def withClient[T](body: RedisClient => T)(implicit tag: TypeTag[T]) = {
    val client = pool.borrowObject
    //TODO: such a bad HACK, however, works for now
    //if result type is future, client can only be returned after the results are there
    try {
      body(client)
    } finally {
      pool.returnObject(client)
    }
  }

  // close pool & free resources
  def close = pool.close
}
