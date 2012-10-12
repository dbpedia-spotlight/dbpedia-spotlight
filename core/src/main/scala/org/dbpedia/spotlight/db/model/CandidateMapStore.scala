package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{Candidate, SurfaceForm}


/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait CandidateMapStore {

  def getCandidates(surfaceform: SurfaceForm): Set[Candidate]

}
