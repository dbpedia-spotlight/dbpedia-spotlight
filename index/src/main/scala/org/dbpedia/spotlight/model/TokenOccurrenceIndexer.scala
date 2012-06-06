package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait TokenOccurrenceIndexer {

  def addTokenOccurrence(resource: DBpediaResource, token: Token, count: Int)
  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Token, Int]])

}
