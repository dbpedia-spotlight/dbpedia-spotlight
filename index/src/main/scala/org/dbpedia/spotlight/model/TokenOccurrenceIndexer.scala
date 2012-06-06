package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait TokenOccurrenceIndexer {

  def add(resource: DBpediaResource, token: Token, count: Int)
  def add(occs: Map[DBpediaResource, Map[Token, Int]])

}
