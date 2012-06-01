package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{Token, DBpediaResource}


/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait ContextStore {

  def get(resource: DBpediaResource, token: Token): Int

}
