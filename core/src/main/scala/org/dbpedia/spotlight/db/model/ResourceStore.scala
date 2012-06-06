package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.DBpediaResource


/**
 * @author Joachim Daiber
 */

trait ResourceStore {

  def getResource(id: Int): DBpediaResource

}
