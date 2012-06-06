package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.db.disk.JDBMStore
import java.io.{FileInputStream, File}
import java.util.Map
import org.dbpedia.spotlight.model._
import scala.Predef.Pair
import org.dbpedia.spotlight.io.SurfaceFormSource
import org.apache.commons.lang.NotImplementedException
import scala.collection.JavaConversions._

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class JDBMStoreIndexer
  extends SurfaceFormIndexer
  with ResourceIndexer
  with TokenIndexer
  with TokenOccurrenceIndexer
  with CandidateIndexer
{

  //SURFACE FORMS

  lazy val sfStore = new JDBMStore[String, Pair[Int, Int]]("resources")

  def addSurfaceForm(sf: SurfaceForm, count: Int) {
    sfStore.add(sf.name, Pair(sf.id, sf.support))
  }

  def addSurfaceForms(sfCount: Map[SurfaceForm, Int]) {
    sfCount.foreach{ case(sf, count) => addSurfaceForm(sf, count) }
    sfStore.commit()
  }


  //DBPEDIA RESOURCES

  lazy val resourceStore = new JDBMStore[Int, DBpediaResource]("resources")

  def addResource(resource: DBpediaResource, count: Int) {
    resourceStore.add(resource.id, resource)
  }

  def addResources(resourceCount: Map[DBpediaResource, Int]) {
    resourceCount.foreach{ case(res, count) => addResource(res, count) }
    resourceStore.commit()
  }


  //RESOURCE CANDIDATES FOR SURFACE FORMS

  lazy val candidateStore = new JDBMStore[Int, List[Pair[Int, Int]]]("candidates")

  def addCandidate(cand: Candidate, count: Int) {
    throw new NotImplementedException()
  }

  def addCandidates(sf: SurfaceForm, cands: List[Pair[Candidate, Int]]) {
    candidateStore.add(cands.head._1.surfaceForm.id, cands.map{ case(cand, count) => Pair(cand.resource.id, count) })
  }

  def addCandidates(cands: Map[Candidate, Int]) {
    cands.toList.groupBy{ case(cand, count) => cand.surfaceForm }.foreach {
      case(sf, candidates) => addCandidates(sf, candidates)
    }
    candidateStore.commit()
  }


  //TOKENS

  lazy val tokenStore = new JDBMStore[String, Pair[Int, Int]]("tokens")

  def addToken(token: Token, count: Int) {
    tokenStore.add(token.name, Pair(token.id, token.count))
  }

  def addTokens(tokenCount: Map[Token, Int]) {
    tokenCount.foreach{ case(token, count) => addToken(token, count) }
    tokenStore.commit()
  }


  //TOKEN OCCURRENCES

  lazy val tokenOccurenceStore = new JDBMStore[Int, Map[Int, Int]]("token_occs")

  def addTokenOccurrence(resource: DBpediaResource, token: Token, count: Int) {
    throw new NotImplementedException()
  }

  def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Token, Int]) {
    //tokenOccurenceStore.add(resource.id, tokenCounts)
  }

  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Token, Int]]) {
    occs.foreach{ case(res, tokenCounts) => addTokenOccurrence(res, tokenCounts) }
    tokenOccurenceStore.commit()
  }

}

object JDBMStoreIndexer {

  def main(args: Array[String]) {
    val indexer = new JDBMStoreIndexer()

    indexer.addSurfaceForms(
      SurfaceFormSource.fromTSVFile(
        new File("/Volumes/Daten/DBpedia/Spotlight/surfaceForms-fromOccs-thresh10-TRD.set")
      ).map{ sf: SurfaceForm => (sf, sf.support) }.toMap.asInstanceOf[java.util.Map[SurfaceForm, Int]]
    )

  }

}