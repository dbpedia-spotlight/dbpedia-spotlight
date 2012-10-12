package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.Token

/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait TokenStore {

  def getToken(token: String): Token
  def getTokenByID(id: Int): Token

}
