package org.dbpedia.spotlight.db.io

import org.dbpedia.spotlight.io.OccurrenceSource
import org.dbpedia.spotlight.db.model.Tokenizer
import org.dbpedia.spotlight.db.memory.{MemoryResourceStore, MemoryTokenStore}
import scala.Int
import java.util.{Map, HashMap}
import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, Token, DBpediaResource, TokenOccurrenceIndexer}


/**
 * @author Joachim Daiber
 *
 *
 *
 */

object IndexTokenOccurrences {

  def index(
    indexer: TokenOccurrenceIndexer,
    occSource: OccurrenceSource,
    tokenStore: MemoryTokenStore,
    tokenizer: Tokenizer,
    resourceStore: MemoryResourceStore,
    flushSize: Int = 1000
  ) {

    indexer.createContextStore(resourceStore.size)

    val resMap = new HashMap[DBpediaResource, Map[Int, Int]]()

    var lastOcc: DBpediaResourceOccurrence = null

    occSource.foreach {
      occ => {
        val res = resourceStore.getResourceByName(occ.resource.uri)

        if (!resMap.containsKey(res)) {
          resMap.put(res, new HashMap[Int, Int]())
        }
        val tokenMap = resMap.get(res)

        tokenizer.tokenize(occ.context) foreach {
          ts: String => {
            val token = tokenStore.getToken(ts)
            var v: Int = tokenMap.get(token.id)
            tokenMap.put(token.id, v + 1)
          }
        }

        //Clear the memory map
        if (lastOcc != occ && resMap.size >= flushSize) {
          indexer.addTokenOccurrences(resMap)
          resMap.clear()
          System.err.println("Flushing...")
        }

        lastOcc = occ
      }
    }
    indexer.writeTokenOccurrences()
  }
}
