package org.dbpedia.spotlight.db.similarity

import org.dbpedia.spotlight.model.{Candidate, Token}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait ContextSimilarity {

  def score(query: java.util.Map[Token, Int], contextCounts: Map[Candidate, java.util.Map[Token, Int]]): Map[Candidate, Double]

}
