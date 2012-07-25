package org.dbpedia.spotlight.io

import io.Source

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 6/10/12
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
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
