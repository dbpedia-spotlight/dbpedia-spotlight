package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait TokenTypeIndexer {

  def addTokenType(token: TokenType, count: Int)
  def addTokenTypes(tokenCount: Map[TokenType, Int])

}
