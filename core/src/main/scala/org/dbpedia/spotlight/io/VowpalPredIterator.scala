package org.dbpedia.spotlight.io

import io.Source

/**
 * Simple iterator implementation that is able to iterate over vowpal predictions from file.
 * @param pathToPredicitons
 */
class VowpalPredIterator(pathToPredicitons: String) extends Iterator[Array[Double]] {
  val iterator = Source.fromFile(pathToPredicitons).getLines()

  override def hasNext: Boolean = iterator.hasNext

  override def next(): Array[Double] = {
    val fileLine = iterator.next()
    val split = fileLine.split(" ")
    val result = new Array[Double](split.length)
    for (i <- 0 until split.length)
      result(i) = split(i).toDouble

    val sum = result.sum

    for (i <- 0 until split.length)
      result(i) /= sum

    result
  }

}
