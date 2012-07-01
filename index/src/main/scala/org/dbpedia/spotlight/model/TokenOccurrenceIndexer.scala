package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait TokenOccurrenceIndexer {

  def addTokenOccurrence(resource: DBpediaResource, token: Token, count: Int)
  def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Int, Int])
  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Int, Int]])
  def addTokenOccurrences(occs: Iterator[Pair[DBpediaResource, Array[Pair[Int, Int]]]])
  def writeTokenOccurrences()
  def createContextStore(n: Int)

}
