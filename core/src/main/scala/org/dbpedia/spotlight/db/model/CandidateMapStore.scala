package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{Candidate, SurfaceForm}


/**
 * A store interface for DBpedia resource candidates for a
 * surface form.
 *
 * @author Joachim Daiber
 */

trait CandidateMapStore {

  /**
   * Returns the set of candidates for a surface form.
   *
   * @param surfaceform the surface form object
   * @return set of candidate objects (includes the DBpedia resource and the co-occurrence count)
   */
  def getCandidates(surfaceform: SurfaceForm): Set[Candidate]

}
