package org.dbpedia.spotlight.db.model

import java.util.Map
import org.dbpedia.spotlight.model.{TokenType, Token, DBpediaResource}


/**
 * A store interface for textual context for DBpedia resources.
 *
 * @author Joachim Daiber
 */

trait ContextStore {

  /**
   * Returns the number of times a token has been observed for
   * the DBpedia resource.
   *
   * @param resource the resource object
   * @param token the token object
   * @return co-occurrence count of resource and token
   */
  def getContextCount(resource: DBpediaResource, token: TokenType): Int


  /**
   * Returns the full token map for a DBpedia resource.
   *
   * @param resource the resource object
   * @return co-occurrence count for all tokens occurring with the DBpedia resource
   */
  def getContextCounts(resource: DBpediaResource): Map[TokenType, Int]

  def getRawContextCounts(resource: DBpediaResource): (Seq[Int], Seq[Int])

  /**
   * Returns the total count of tokens occurring together with the DBpedia resource.
   *
   * @param resource the resource object
   * @return total count of tokens for the DBpedia resource
   */
  def getTotalTokenCount(resource: DBpediaResource): Int




}
