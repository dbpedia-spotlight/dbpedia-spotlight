package org.dbpedia.spotlight.db

import disk.JDBMStore
import memory._
import org.apache.commons.lang.NotImplementedException
import java.lang.{Short, String}


import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import collection.mutable.ListBuffer
import java.util.{Map, Set}
import scala.Array
import java.io.File
import org.dbpedia.spotlight.model._

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class MemoryStoreIndexer(val baseDir: File)
  extends SurfaceFormIndexer
  with ResourceIndexer
  with CandidateIndexer
  with TokenIndexer
  with TokenOccurrenceIndexer
{

  //SURFACE FORMS

  def addSurfaceForm(sf: SurfaceForm, count: Int) {
    throw new NotImplementedException()
  }

  def addSurfaceForms(sfCount: Map[SurfaceForm, Int]) {
    addSurfaceForms(sfCount.toIterator)
  }

  def addSurfaceForms(sfCount: Iterator[Pair[SurfaceForm, Int]]) {
    val sfStore = new MemorySurfaceFormStore()

    val supportForID = ListBuffer[Int]()
    val stringForID  = ListBuffer[String]()

    sfCount foreach {
      case (sf, count) => {
        stringForID.append(sf.name)
        supportForID.append(count)
      }
    }

    sfStore.stringForID  = stringForID.toArray
    sfStore.supportForID = supportForID.toArray
    MemoryStore.dump(sfStore, new File(baseDir, "sf.mem"))
  }


  //RESOURCES

  def addResource(resource: DBpediaResource, count: Int) {
    throw new NotImplementedException()
  }

  def addResources(resourceCount: Map[DBpediaResource, Int]) {
    val resStore = new MemoryResourceStore()

    val ontologyTypeStore = MemoryStoreIndexer.createOntologyTypeStore(
      resourceCount.keys.flatMap(_.getTypes).toSet.asJava
    )

    val supportForID = new Array[Int](resourceCount.size+1)
    val uriForID = new Array[String](resourceCount.size+1)
    val typesForID = new Array[Array[Short]](resourceCount.size+1)

    resourceCount.foreach {

      // (res, count)
      (el: (DBpediaResource, Int)) => {

        supportForID(el._1.id) = el._1.support
        uriForID(el._1.id) = el._1.uri
        typesForID(el._1.id) = (el._1.getTypes map {
          ot: OntologyType => ontologyTypeStore.getOntologyTypeByName(ot.typeID).id}
          ).toArray

      }
    }

    resStore.ontologyTypeStore = ontologyTypeStore
    resStore.supportForID = supportForID.array
    resStore.uriForID = uriForID.array
    resStore.typesForID = typesForID.array

    MemoryStore.dump(resStore, new File(baseDir, "res.mem"))
  }




  def addCandidate(cand: Candidate, count: Int) {
    throw new NotImplementedException()
  }

  def addCandidates(cands: Map[Candidate, Int], numberOfSurfaceForms: Int) {
    val candmapStore = new MemoryCandidateMapStore()

    val candidates      = new Array[ListBuffer[Int]](numberOfSurfaceForms)
    val candidateCounts = new Array[ListBuffer[Int]](numberOfSurfaceForms)

    cands.foreach {
      p: (Candidate, Int) => {
        if(candidates(p._1.surfaceForm.id) == null) {
          candidates(p._1.surfaceForm.id)      = ListBuffer[Int]()
          candidateCounts(p._1.surfaceForm.id) = ListBuffer[Int]()
        }

        candidates(p._1.surfaceForm.id)      += p._1.resource.id
        candidateCounts(p._1.surfaceForm.id) += p._2
      }
    }

    candmapStore.candidates = (candidates map { l: ListBuffer[Int] => if(l != null) l.toArray else null} ).toArray
    candmapStore.candidateCounts = (candidateCounts map { l: ListBuffer[Int] => if(l != null) l.toArray else null} ).toArray

    MemoryStore.dump(candmapStore, new File(baseDir, "candmap.mem"))
  }

  def addCandidatesByID(cands: Map[Pair[Int, Int], Int], numberOfSurfaceForms: Int) {
    val candmapStore = new MemoryCandidateMapStore()

    val candidates      = new Array[Array[Int]](numberOfSurfaceForms)
    val candidateCounts = new Array[Array[Int]](numberOfSurfaceForms)

    cands.foreach {
      p: (Pair[Int, Int], Int) => {

        if(candidates(p._1._1) == null) {
          candidates(p._1._1)      = Array[Int]()
          candidateCounts(p._1._1) = Array[Int]()
        }

        candidates(p._1._1)      :+= p._1._2
        candidateCounts(p._1._1) :+= p._2
      }
    }

    candmapStore.candidates = candidates
    candmapStore.candidateCounts = candidateCounts

    MemoryStore.dump(candmapStore, new File(baseDir, "candmap.mem"))
  }

  def addToken(token: Token, count: Int) {
    throw new NotImplementedException()
  }

  def addTokens(tokenCount: Map[Token, Int]) {

    val tokenStore = new MemoryTokenStore()

    val tokens = new Array[String](tokenCount.size)
    val counts = new Array[Int](tokenCount.size)

    tokenCount.foreach {
      case (token, count) => {
        tokens(token.id) = token.name
        counts(token.id) = count
      }
    }

    tokenStore.tokenForId = tokens.array
    tokenStore.counts = counts.array

    MemoryStore.dump(tokenStore, new File(baseDir, "tokens.mem"))
  }


  //TOKEN OCCURRENCES

  def addTokenOccurrence(resource: DBpediaResource, token: Token, count: Int) {
    throw new NotImplementedException()
  }

  def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Int, Int]) {
    throw new NotImplementedException()
  }

  lazy val contextStore = new MemoryContextStore()

  def createContextStore(n: Int) {
    contextStore.tokens = new Array[Array[Int]](n)
    contextStore.counts = new Array[Array[Int]](n)
  }

  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Int, Int]]) {
    occs.foreach{ case(res, tokenCounts) => {
      val (t, c) = tokenCounts.unzip
      contextStore.tokens(res.id) = t.toArray
      contextStore.counts(res.id) = c.toArray
    }
    }
  }

  def addTokenOccurrences(occs: Iterator[Triple[DBpediaResource, Array[Token], Array[Int]]]) {
    occs.foreach{
      case(res, tokens, counts) => {
        contextStore.tokens(res.id) = tokens.map{ t: Token => t.id }.array
        contextStore.counts(res.id) = counts.array
      }
    }
  }


  def writeTokenOccurrences() {
    MemoryStore.dump(contextStore, new File(baseDir, "context.mem"))
  }


}

object MemoryStoreIndexer {

  def createOntologyTypeStore(types: Set[OntologyType]): MemoryOntologyTypeStore = {
    var idFromName = new java.util.HashMap[String, Short]()
    var ontologyTypeFromID = new java.util.HashMap[Short, OntologyType]()

    var i = 0.toShort
    types foreach {
      ontologyType: OntologyType =>
        ontologyType.id = i

        ontologyTypeFromID.put(ontologyType.id, ontologyType)
        idFromName.put(ontologyType.typeID, ontologyType.id)

        i = (i + 1).toShort
    }

    val otStore = new MemoryOntologyTypeStore()
    otStore.idFromName = idFromName
    otStore.ontologyTypeFromID = ontologyTypeFromID

    otStore
  }

}
