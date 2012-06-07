package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.model.{Candidate, SurfaceForm}
import org.dbpedia.spotlight.db.model.{ResourceStore, CandidateMapStore}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class MemoryCandidateMapStore(resourceStore: ResourceStore)
  extends MemoryStore
  with CandidateMapStore {

  var candidates = Array[Array[Pair[Int, Int]]]()

  def getCandidates(surfaceform: SurfaceForm): Set[Candidate] =
    candidates(surfaceform.id).map {
      //TODO add count
      case (resID, count) => new Candidate(surfaceform, resourceStore.getResource(resID))
    }.toSet

}
