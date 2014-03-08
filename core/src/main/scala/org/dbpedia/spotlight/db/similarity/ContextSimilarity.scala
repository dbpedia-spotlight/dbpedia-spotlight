package org.dbpedia.spotlight.db.similarity

import collection.mutable
import org.dbpedia.spotlight.model.{TokenType, DBpediaResource}


/**
 * A context similarity calculates a score for DBpedia resource candidates given its surrounding textual context.
 *
 * @author Joachim Daiber
 */

trait ContextSimilarity {

  /**
   * Calculate the context score for all DBpedia resources in the given text. The text context is specified
   * as q query of tokens and their counts.
   *
   * @param query the text context of the document
   * @param candidates the set of DBpedia resource candidates
   * @return
   */
  def score(query: Seq[TokenType], candidates: Set[DBpediaResource]): mutable.Map[DBpediaResource, Double]


  /**
   * Calculate the context score for the context alone, not assuming that there is any entity generating it.
   *
   * In the generative model, this is: \product_i P_LM(token_i)
   *
   * @param query the text context of the document
   * @return
   */
  def nilScore(query: Seq[TokenType]): Double

}