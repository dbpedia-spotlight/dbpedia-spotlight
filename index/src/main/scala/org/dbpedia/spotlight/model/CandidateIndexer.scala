package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 */

trait CandidateIndexer {

  def addCandidate(cand: Candidate, count: Int)
  def addCandidates(cands: Map[Candidate, Int])

}
