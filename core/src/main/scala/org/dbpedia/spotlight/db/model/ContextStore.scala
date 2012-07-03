package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{Token, DBpediaResource}
import java.util.Map


/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait ContextStore {

  def getContextCount(resource: DBpediaResource, token: Token): Int
  def getContextCounts(resource: DBpediaResource): Map[Token, Int]


}
