package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.db.disk.JDBMStore
import java.io.File
import org.apache.commons.lang.NotImplementedException
import scala.collection.JavaConversions._

import scala.Predef._
import scala._
import java.util.Map
import org.dbpedia.spotlight.model._

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
  with TokenOccurrenceIndexer
  with CandidateIndexer
{

  //SURFACE FORMS

  lazy val sfStore = new JDBMStore[String, Pair[Int, Int]](new File(baseDir, "sf.disk").getAbsolutePath)

  def addSurfaceForm(sf: SurfaceForm, annotatedCount: Int, totalCount: Int) {
    sfStore.add(sf.name, Pair(sf.id, sf.annotatedCount))
  }

  def addSurfaceForms(sfCount: Map[SurfaceForm, (Int, Int)]) {
    sfCount.foreach{ case(sf, count) => addSurfaceForm(sf, count._1, count._2) }
    sfStore.commit()
  }

  def addSurfaceForms(sfCount: Iterator[Pair[SurfaceForm, (Int, Int)]]) {
    sfCount.foreach{ case(sf, count) => addSurfaceForm(sf, count._1, count._2) }
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

  def addCandidatesByID(cands: Map[Pair[Int, Int], Int], numberOfSurfaceForms: Int) {
    throw new NotImplementedException()
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
  lazy val contextStore = new JDBMStore[Int, Map[Int, Int]](new File(baseDir, "context.disk").getAbsolutePath)

  def addTokenOccurrence(resource: DBpediaResource, token: Token, count: Int) {
    throw new NotImplementedException()
  }

  def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Int, Int]) {
    contextStore.add(resource.id, tokenCounts)
  }

  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Int, Int]]) {
    occs.foreach{ case(res, tokenCounts) => addTokenOccurrence(res, tokenCounts) }
    writeTokenOccurrences()
  }

  def addTokenOccurrences(occs: Iterator[Triple[DBpediaResource, Array[Token], Array[Int]]]) {
    throw new NotImplementedException()
  }

  def createContextStore(n: Int) = {
    //
  }

  def writeTokenOccurrences() {
    contextStore.commit()
  }

}