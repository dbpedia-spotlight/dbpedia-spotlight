package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.QuantizedCountStore
import scala.collection.mutable

class MemoryQuantizedCountStore extends MemoryStore with QuantizedCountStore {

  var countMap: java.util.Map[Short, Int] = new java.util.HashMap[Short, Int]()

  def getCount(quantized: Short): Int = countMap.get(quantized)

  @transient
  var countLookup: mutable.HashMap[Int, Short] = null

  def addCount(count: Int): Short = {

    if(countLookup == null)
      countLookup = mutable.HashMap[Int, Short]()

    countLookup.get(count) match {
      case Some(s) => s
      case None => {
        val s = (Short.MinValue + 100 + countMap.size()).toShort
        countMap.put(s, count)
        countLookup.put(count, s)
        s
      }
    }
  }

  def size: Int = countMap.size()


}
