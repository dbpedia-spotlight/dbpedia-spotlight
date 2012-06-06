package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait TokenOccurrenceIndexer {

  def addTokenOccurrence(resource: DBpediaResource, token: Token, count: Int)
  def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Token, Int])
  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Token, Int]])

}
