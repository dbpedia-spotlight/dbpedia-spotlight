package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait TokenOccurrenceIndexer {

  def addTokenOccurrence(resource: DBpediaResource, token: TokenType, count: Int)
  def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Int, Int])
  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Int, Int]])
  def addTokenOccurrences(occs: Iterator[Triple[DBpediaResource, Array[TokenType], Array[Int]]])
  def writeTokenOccurrences()
  def createContextStore(n: Int)

}
