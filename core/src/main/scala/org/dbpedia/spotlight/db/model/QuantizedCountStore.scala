package org.dbpedia.spotlight.db.model

trait QuantizedCountStore {

  def getCount(quantized: Short): Int
  def addCount(count: Int): Short

}
