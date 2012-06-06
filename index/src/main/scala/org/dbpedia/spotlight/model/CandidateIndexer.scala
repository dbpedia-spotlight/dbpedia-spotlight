package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 */

trait CandidateIndexer {

  def add(cand: Candidate, count: Int)
  def add(cands: Map[Candidate, Int])

}
