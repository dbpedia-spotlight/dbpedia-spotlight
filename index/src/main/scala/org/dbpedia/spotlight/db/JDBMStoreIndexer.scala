package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.db.disk.JDBMStore
import java.io.File
import org.apache.commons.lang.NotImplementedException

import scala.Predef._
import scala._
import java.util.Map
import org.dbpedia.spotlight.model._

/**
 * Implements indexing for JDBM disk-based storage.
 *
 * @author Joachim Daiber
 */

class JDBMStoreIndexer(val baseDir: File)
  extends TokenOccurrenceIndexer
{

  //Token OCCURRENCES
  lazy val contextStore = new JDBMStore[Int, Triple[Array[Int], Array[Int], Int]](new File(baseDir, "context.disk").getAbsolutePath)

  def addTokenOccurrence(resource: DBpediaResource, token: TokenType, count: Int) {
    throw new NotImplementedException()
  }

  def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Int, Int]) {
    throw new NotImplementedException()
  }


  def addTokenOccurrences(occs: Iterator[Triple[DBpediaResource, Array[TokenType], Array[Int]]]) {

    occs.filter(t => t!=null && t._1 != null).foreach{
      t: Triple[DBpediaResource, Array[TokenType], Array[Int]] => {
        val Triple(res, tokens, counts) = t
        if (res != null) {
          assert (tokens.size == counts.size)
          if(contextStore.get(res.id) != null) {
            val Triple(existingTokens, exisitingCounts, _) = contextStore.get(res.id)

            val (mergedTokens, mergedCounts) = (tokens.map{ t: TokenType => t.id }.array.zip(counts.array) ++ existingTokens.zip( exisitingCounts )).groupBy(_._1).map{ case(k, v) => (k, v.map{ p => p._2}.sum ) }.unzip
            contextStore.add(res.id, Triple(mergedTokens.toArray.array, mergedCounts.toArray.array, mergedCounts.sum))
          } else{
            contextStore.add(res.id, Triple(tokens.map{ t: TokenType => t.id }.array, counts.array, counts.sum))
          }
        }
      }
    }

    writeTokenOccurrences()
  }

  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Int, Int]]) {
    throw new NotImplementedException()
  }

  def createContextStore(n: Int) {
    //
  }

  def writeTokenOccurrences() {
    contextStore.commit()
  }

}