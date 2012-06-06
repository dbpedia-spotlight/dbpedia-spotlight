package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait ResourceIndexer {

  def add(resource: DBpediaResource, count: Int)
  def add(resourceCount: Map[DBpediaResource, Int])

}
