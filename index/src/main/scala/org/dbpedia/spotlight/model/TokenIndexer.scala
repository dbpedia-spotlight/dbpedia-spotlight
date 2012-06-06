package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait TokenIndexer {

  def addToken(token: Token, count: Int)
  def addTokens(tokenCount: Map[Token, Int])

}
