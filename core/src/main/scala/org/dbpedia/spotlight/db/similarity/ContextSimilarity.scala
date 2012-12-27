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
   * @param contextCounts the counts for all tokens in the context of the DBpedia resources
   * @param totalContextCounts total count of the tokens in the context of a DBpedia resource
   * @return
   */
  def score(query: java.util.Map[TokenType, Int], contextCounts: Map[DBpediaResource, java.util.Map[TokenType, Int]], totalContextCounts: Map[DBpediaResource, Int]): mutable.Map[DBpediaResource, Double]

}
