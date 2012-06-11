package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.db.disk.JDBMStore
import java.io.File
import java.util.Map
import org.dbpedia.spotlight.model._
import org.apache.commons.lang.NotImplementedException
import scala.collection.JavaConversions._
import scala.Predef._

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class JDBMStoreIndexer(val baseDir: File)
  extends SurfaceFormIndexer
  with ResourceIndexer
  with TokenIndexer
  //with OccurrenceIndexer
  with CandidateIndexer
{

  //SURFACE FORMS

  lazy val sfStore = new JDBMStore[String, Pair[Int, Int]](new File(baseDir, "sf.disk").getAbsolutePath)

  def addSurfaceForm(sf: SurfaceForm, count: Int) {
    sfStore.add(sf.name, Pair(sf.id, sf.support))
  }

  def addSurfaceForms(sfCount: Map[SurfaceForm, Int]) {
    sfCount.foreach{ case(sf, count) => addSurfaceForm(sf, count) }
    sfStore.commit()
  }

  def addSurfaceForms(sfCount: Iterator[Pair[SurfaceForm, Int]]) {
    sfCount.foreach{ case(sf, count) => addSurfaceForm(sf, count) }
    sfStore.commit()
  }



  //DBPEDIA RESOURCES

  lazy val resourceStore = new JDBMStore[Int, DBpediaResource](new File(baseDir, "res.disk").getAbsolutePath)

  def addResource(resource: DBpediaResource, count: Int) {
    resourceStore.add(resource.id, resource)
  }

  def addResources(resourceCount: Map[DBpediaResource, Int]) {
    resourceCount.foreach{ case(res, count) => addResource(res, count) }
    resourceStore.commit()
  }


  //RESOURCE CANDIDATES FOR SURFACE FORMS

  lazy val candidateStore = new JDBMStore[Int, List[Pair[Int, Int]]](new File(baseDir, "cand.disk").getAbsolutePath)

  def addCandidate(cand: Candidate, count: Int) {
    throw new NotImplementedException()
  }

  def addCandidates(sf: SurfaceForm, cands: List[Pair[Candidate, Int]]) {
    candidateStore.add(cands.head._1.surfaceForm.id, cands.map{ case(cand, count) => Pair(cand.resource.id, count) })
  }

  def addCandidates(cands: Map[Candidate, Int], numberOfSurfaceForms: Int) {
    cands.toList.groupBy{ case(cand, count) => cand.surfaceForm }.foreach {
      case(sf, candidates) => addCandidates(sf, candidates)
    }
    candidateStore.commit()
  }


  //TOKENS

  lazy val tokenStore = new JDBMStore[String, Pair[Int, Int]](new File(baseDir, "tokens.disk").getAbsolutePath)

  def addToken(token: Token, count: Int) {
    tokenStore.add(token.name, Pair(token.id, token.count))
  }

  def addTokens(tokenCount: Map[Token, Int]) {
    tokenCount.foreach{ case(token, count) => addToken(token, count) }
    tokenStore.commit()
  }

  //DBPEDIA RESOURCE OCCURRENCES
  //lazy val contextStore = new JDBMStore[Int, Map[Int, Int]]



  //TOKEN OCCURRENCES

  //lazy val tokenOccurrenceStore = new JDBMStore[Int, Map[Int, Int]](new File(baseDir, "token_occs.disk").getAbsolutePath)

 // def addTokenOccurrence(resource: DBpediaResource, token: Token, count: Int) {
 //   throw new NotImplementedException()
 // }
//
 // def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Token, Int]) {
 //   tokenOccurrenceStore.add(resource.id, tokenCounts.map{ case(token, count) => (token.id, count) }.toMap)
 // }
//
 // def addTokenOccurrences(occs: Map[DBpediaResource, Map[Token, Int]]) {
 //   occs.foreach{ case(res, tokenCounts) => addTokenOccurrence(res, tokenCounts) }
 //   tokenOccurrenceStore.commit()
 // }

}