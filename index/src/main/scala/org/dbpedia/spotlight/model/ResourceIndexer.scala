package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait ResourceIndexer {

  def addResource(resource: DBpediaResource, count: Int)
  def addResources(resourceCount: Map[DBpediaResource, Int])

}
