package org.dbpedia.spotlight.storage

import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration.Duration
import scala.collection.mutable
import java.util.Properties

/**
 * @author dirk
 *         Date: 4/4/14
 *         Time: 11:33 AM
 */
trait CountStore {
  def get(k: String): Option[Int]

  def multiGet(ks: scala.collection.Set[String]): scala.collection.Map[String, Option[Int]]

  def merge(kv: (String, Int)): Int

  def multiMerge(kvs: scala.collection.Map[String, Int]): scala.collection.Map[String, Int]

  def put(kv: (String, Int)): Boolean

  def multiPut(kvs: scala.collection.Map[String, Int]): Boolean

  def close()

  def clear()

}

object CountStore {
  private var countStores = mutable.Map[String, CountStore]()

  def cachedOrFromConfig(name: String, conf: CountStoreConfiguration = new CountStoreConfiguration(), typ: CountStoreType = RedisCountStore) = {
    countStores.synchronized {
      if (!countStores.contains(name))
        countStores += name -> typ.fromConfig(conf)
    }
    countStores(name)
  }
}

class CountStoreConfiguration extends Properties {
  def setHost(host: String) = setProperty("host", host)

  def setPort(port: Int) = setProperty("port", port.toString)

  def getHost = getProperty("host", "localhost")

  def getPort = getProperty("port", "-1").toInt
}

trait CountStoreType {
  def fromConfig(conf: CountStoreConfiguration): CountStore
}