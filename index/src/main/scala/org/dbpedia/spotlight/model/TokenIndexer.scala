package org.dbpedia.spotlight.model

import java.util.List

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait TokenIndexer {

  def add(token: Token, count: Int)
  def add(tokenCount: Map[Token, Int])

}
