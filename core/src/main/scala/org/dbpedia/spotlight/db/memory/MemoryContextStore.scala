package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.ContextStore
import org.dbpedia.spotlight.model.{Token, DBpediaResource}
import org.apache.commons.lang.NotImplementedException
import java.util.{Map, HashMap}


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

  var tokens: Array[Array[Int]] = null
  var counts: Array[Array[Int]] = null

  def size = tokens.length

  def getContextCount(resource: DBpediaResource, token: Token): Int = {
    throw new NotImplementedException()
  }

  def getContextCounts(resource: DBpediaResource): Map[Int, Int] = {

    val contextCounts = new HashMap[Int, Int]()

    if (tokens(resource.id) != null) {
      val t = tokens(resource.id)
      val c = counts(resource.id)

      (0 to t.length) foreach { i =>
        contextCounts.put(t(i), c(i))
      }
    }

    contextCounts
  }



}
