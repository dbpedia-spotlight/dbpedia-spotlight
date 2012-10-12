package org.dbpedia.spotlight.db.disk

import org.dbpedia.spotlight.model.{Token, DBpediaResource, SurfaceForm}
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException
import java.util.{HashMap, Map}
import collection.JavaConversions._
import org.dbpedia.spotlight.db.model.{TokenStore, ContextStore}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class DiskContextStore(file: String) extends ContextStore
{

  @transient
  var tokenStore: TokenStore = null

  val jdbm = new JDBMStore[Int, Map[Int, Int]](file)

  def getContextCount(resource: DBpediaResource, token: Token): Int = {
    val resMap = jdbm.get(resource.id)
    if (resMap == null)
      throw new DBpediaResourceNotFoundException("Resource not found.")

    if (resMap.containsKey(token.id)) {
      resMap.get(token.id)
    } else {
      0
    }
  }

  def getContextCounts(resource: DBpediaResource): Map[Token, Int] = {
    val resMap = jdbm.get(resource.id)
    if (resMap == null)
      throw new DBpediaResourceNotFoundException("Resource not found.")

    val resTokenMap = new HashMap[Token, Int]()
    resMap.foreach {
      case (tokenID, count) => resTokenMap.put(tokenStore.getTokenByID(tokenID), count)
    }
    resTokenMap
  }


}
