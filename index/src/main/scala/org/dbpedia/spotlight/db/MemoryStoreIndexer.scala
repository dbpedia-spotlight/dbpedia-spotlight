package org.dbpedia.spotlight.db

import memory._
import model.OntologyTypeStore
import org.apache.commons.lang.NotImplementedException
import gnu.trove.TObjectIntHashMap
import java.io.File
import org.dbpedia.spotlight.model._
import java.lang.{Short, String}


import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import collection.mutable.{WrappedArray, HashMap, ListBuffer}
import java.util.{ArrayList, HashMap, Map, Set}
import scala.Array

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

    val supportForID = new Array[Int](resourceCount.size)
    val uriForID = new Array[String](resourceCount.size)
    val typesForID = new Array[Array[Short]](resourceCount.size)

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


    println(uriForID.filter({ s: String => s == null }).size)

    resStore.ontologyTypeStore = ontologyTypeStore
    resStore.supportForID = supportForID.array
    resStore.uriForID = uriForID.array
    resStore.typesForID = typesForID.array

    println(resStore.uriForID.filter({ s: String => s == null }).size)

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