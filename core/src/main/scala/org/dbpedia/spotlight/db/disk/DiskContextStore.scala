package org.dbpedia.spotlight.db.disk

import org.dbpedia.spotlight.db.model.ContextStore
import org.dbpedia.spotlight.model.{Token, DBpediaResource, SurfaceForm}
import java.util.Map
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class DiskContextStore(file: String) extends ContextStore
{

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

  def getContextCounts(resource: DBpediaResource): Map[Int, Int] = {
    val resMap = jdbm.get(resource.id)
    if (resMap == null)
      throw new DBpediaResourceNotFoundException("Resource not found.")

    resMap
  }


}
