package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.model.{Token, DBpediaResource}
import org.apache.commons.lang.NotImplementedException
import java.util.{Map, HashMap}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.db.model.{TokenStore, ContextStore}


/**
 * @author Joachim Daiber
 *
 *
 *
 */

@SerialVersionUID(1007001)
class MemoryContextStore
  extends MemoryStore
  with ContextStore {

  @transient
  var tokenStore: TokenStore = null

  var tokens: Array[Array[Int]] = null
  var counts: Array[Array[Int]] = null

  def size = tokens.length

  def getContextCount(resource: DBpediaResource, token: Token): Int = {
    throw new NotImplementedException()
  }

  def getContextCounts(resource: DBpediaResource): Map[Token, Int] = {

    val contextCounts = new HashMap[Token, Int]()
  	val i = resource.id +1

    if (tokens(i) != null) {
      val t = tokens(i)
      val c = counts(i)
		println(t.size, c.size)
      (0 to t.length-1) foreach { j =>
        contextCounts.put(tokenStore.getTokenByID(t(j)), c(j))
      }
    }

    contextCounts
  }



}
