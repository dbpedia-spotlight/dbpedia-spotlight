package org.dbpedia.spotlight.db

import model.{ResourceStore, SurfaceFormStore, CandidateMapStore}
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException

/**
 * A database-backed candidate searcher to retrieve candidates for a surface form.
 *
 * TODO: this does not extend [[org.dbpedia.spotlight.model.CandidateSearcher]] at the moment, because
 * we need to return [[org.dbpedia.spotlight.model.Candidate]] objects instead of DBpedia resources
 * (candidate objects may include additional data, e.g. c(res, sf) )
 *
 * @author Joachim Daiber
 */

class DBCandidateSearcher(val resStore: ResourceStore, val sfStore: SurfaceFormStore,  candidateMap: CandidateMapStore) {

  /**
   * Retrieves all DBpedia Resources that can be confused with surface form sf.
   *
   * @param sf the surface form
   * @return
   */
  def getCandidates(sf: SurfaceForm): Set[Candidate] = {
    val cands = candidateMap.getCandidates(sf)
    try {
      if (cands.size == 0)
        candidateMap.getCandidates(sfStore.getSurfaceFormNormalized(sf.name))
      else
        cands
    } catch {
      case e: SurfaceFormNotFoundException => cands
    }
  }


  /**
   * Retrieves the number of DBpedia Resources that can be confused with surface form sf.
   *
   * @param sf the surface form
   * @return
   */
  def getAmbiguity(sf: SurfaceForm): Int = getCandidates(sf).size


}
