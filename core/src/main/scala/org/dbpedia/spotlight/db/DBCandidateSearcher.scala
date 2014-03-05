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

class DBCandidateSearcher(val resStore: ResourceStore, val sfStore: SurfaceFormStore, candidateMap: CandidateMapStore) {

  val ADD_TOP_NORMALIZED_SFS = 5

  /**
   * Retrieves all DBpedia Resources that can be confused with surface form sf.
   *
   * @param sf the surface form
   * @return
   */
  def getCandidates(sf: SurfaceForm): Set[Candidate] = {

    var cands = Set[Candidate]()

    if(sf.id > 0)
      cands ++= candidateMap.getCandidates(sf)
    else
      try {
        cands ++= candidateMap.getCandidates(sfStore.getSurfaceForm(sf.name))
      } catch {
        case e: SurfaceFormNotFoundException =>
      }

    if (cands.size == 0)
      sfStore.getRankedSurfaceFormCandidates(sf.name).take(ADD_TOP_NORMALIZED_SFS).foreach(p =>
        cands ++= candidateMap.getCandidates(p._1)
      )

    cands
  }


  /**
   * Retrieves the number of DBpedia Resources that can be confused with surface form sf.
   *
   * @param sf the surface form
   * @return
   */
  def getAmbiguity(sf: SurfaceForm): Int = {
    try {
      candidateMap.getCandidates(sfStore.getSurfaceForm(sf.name)).size
    } catch {
      case _: Throwable => 0
    }
  }


}
