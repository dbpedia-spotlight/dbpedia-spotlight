package org.dbpedia.spotlight.db.disk

import java.util.{HashMap, Map}
import org.dbpedia.spotlight.db.model.{TokenTypeStore, ContextStore}
import org.dbpedia.spotlight.model.{TokenType, DBpediaResource}
import org.apache.commons.lang.NotImplementedException

/**
 * A disk-based [[org.dbpedia.spotlight.db.model.ContextStore]] based on JDBM.
 * Caching is active by default (LRU cache).
 *
 * @author Joachim Daiber
 */

class DiskContextStore(file: String) extends ContextStore
{

  @transient
  var tokenTypeStore: TokenTypeStore = null

  val jdbm = new JDBMStore[Int, Triple[Array[Int], Array[Int], Int]](file)

  def getContextCount(resource: DBpediaResource, token: TokenType): Int = {
    throw new NotImplementedException()
  }

  def getTotalTokenCount(resource: DBpediaResource): Int = {
    val resTriple = jdbm.get(resource.id)

    if(resTriple != null && resTriple._1 != null && resTriple._2 != null)
      resTriple._3
    else
      0
  }

  def getRawContextCounts(resource: DBpediaResource): (Seq[Int], Seq[Int]) = {
    throw new NotImplementedException()
  }

  def getContextCounts(resource: DBpediaResource): Map[TokenType, Int] = {
    val resTriple   = jdbm.get(resource.id)
    val resTokenMap = new HashMap[TokenType, Int]()

    if(resTriple != null && resTriple._1 != null && resTriple._2 != null)
      resTriple._1.zip(resTriple._2).foreach {
        case (tokenID, count) => resTokenMap.put(tokenTypeStore.getTokenTypeByID(tokenID), count)
        case _ =>
      }

    resTokenMap
  }


}
