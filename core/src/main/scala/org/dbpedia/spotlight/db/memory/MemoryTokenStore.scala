package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.TokenStore
import org.dbpedia.spotlight.model.Token
import gnu.trove.TObjectIntHashMap

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class MemoryTokenStore
  extends MemoryStore
  with TokenStore
{

  var tokenToID: TObjectIntHashMap = null
  var counts: Array[Int] = null

  def getToken(token: String): Token = {

    val normalizedToken = normalize(token)
    val id = tokenToID.get(normalizedToken)
    val count = counts(id)

    new Token(id, normalizedToken, count)
  }


}
