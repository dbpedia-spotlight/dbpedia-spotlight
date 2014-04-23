package org.dbpedia.spotlight.storage

import scala.collection.mutable
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{ExecutionContext, Awaitable, Await, Future}
import scala.concurrent.duration.Duration

/**
 * @author dirk
 *         Date: 4/9/14
 *         Time: 5:27 PM
 */
class LocalCountStore(implicit executionContext: ExecutionContext) extends CountStore {
  private val map = new ConcurrentHashMap[String, Int]()

  def get(k: String) = {
    val res = map.get(k); if (res == 0) None else Some(res)
  }

  def multiGet(ks: scala.collection.Set[String]) = ks.foldLeft(mutable.Map[String, Option[Int]]())((acc, k) => {
    acc += k -> get(k); acc
  })

  def merge(kv: (String, Int)) = {
    val r = kv._2 + map.get(kv._1); map.put(kv._1, r); r
  }

  def multiMerge(kvs: scala.collection.Map[String, Int]) = {
    kvs.map(kv => kv._1 -> merge(kv._1, kv._2))
  }

  def put(kv: (String, Int)) = {
    if (kv._2 == 0) map.remove(kv._1) else map.put(kv._1, kv._2); true
  }

  def multiPut(kvs: scala.collection.Map[String, Int]) = {
    kvs.map(kv => put(kv)).reduce(_ && _)
  }

  def close() = {}

  def clear() = map.clear()
}

object LocalCountStore extends CountStoreType {
  def fromConfig(conf: CountStoreConfiguration) = new LocalCountStore()(ExecutionContext.global)
}
