package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.Token

/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait TokenStore {

  def get(token: String): Token

}
