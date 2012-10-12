package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.DBpediaResource
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException
import scala.throws


/**
 * @author Joachim Daiber
 */

trait ResourceStore {

  @throws(classOf[DBpediaResourceNotFoundException])
  def getResource(id: Int): DBpediaResource

  @throws(classOf[DBpediaResourceNotFoundException])
  def getResourceByName(name: String): DBpediaResource

}
