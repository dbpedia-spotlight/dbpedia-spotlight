package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.model.{Candidate, SurfaceForm}
import org.dbpedia.spotlight.db.model.{ResourceStore, CandidateMapStore}
import scala.Array

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class MemoryCandidateMapStore
  extends MemoryStore
  with CandidateMapStore {

  private val serialVersionUID = 101010106

  var candidates      = Array[Array[Int]]()
  var candidateCounts = Array[Array[Int]]()

  def size = candidates.size

  @transient
  var resourceStore: ResourceStore = null

  def getCandidates(surfaceform: SurfaceForm): Set[Candidate] =
    candidates(surfaceform.id).zip(candidateCounts(surfaceform.id)).map {
      case (resID, count) => new Candidate(surfaceform, resourceStore.getResource(resID), count)
    }.toSet

}
