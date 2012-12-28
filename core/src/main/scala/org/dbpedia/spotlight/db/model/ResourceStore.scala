package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.DBpediaResource
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException
import scala.throws


/**
 * A store interface for DBpedia resources.
 *
 * @author Joachim Daiber
 */

trait ResourceStore {

  /**
   * Returns the DBpedia resource corresponding to the internal ID.
   *
   * @param id internal ID of the DBpedia resource
   * @throws org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException
   * @return
   */
  @throws(classOf[DBpediaResourceNotFoundException])
  def getResource(id: Int): DBpediaResource


  /**
   * Returns the DBpedia resource corresponding to the specified name.
   * The name is the last part of the full DBpedia URI.
   *
   * @param name URI identifier of the DBpedia resource (without namespace)
   * @throws org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException
   * @return
   */
  @throws(classOf[DBpediaResourceNotFoundException])
  def getResourceByName(name: String): DBpediaResource

}
