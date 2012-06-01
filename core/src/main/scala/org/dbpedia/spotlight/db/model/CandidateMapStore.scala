package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{Candidate, SurfaceForm}


/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait CandidateMapStore {

  def get(surfaceform: SurfaceForm): Set[Candidate]

}
