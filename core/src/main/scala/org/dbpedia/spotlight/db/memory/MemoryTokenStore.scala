package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.TokenStore
import org.dbpedia.spotlight.model.Token
import gnu.trove.TObjectIntHashMap
import java.lang.String

/**
 * @author Joachim Daiber
 *
 *
 *
 */

@SerialVersionUID(1006001)
class MemoryTokenStore
  extends MemoryStore
  with TokenStore
{

  var tokenForId: Array[String] = null
  var counts: Array[Int] = null

  @transient
  var idFromToken: TObjectIntHashMap = null

  override def loaded() {
    createReverseLookup()
  }

  def size = tokenForId.size

  def createReverseLookup() {
    if (tokenForId != null) {
      System.err.println("Creating reverse-lookup for Tokens.")
      idFromToken = new TObjectIntHashMap(tokenForId.size)

      var id = 0
      tokenForId foreach { token => {
        idFromToken.put(token, id)
        id += 1
       }
      }
    }
  }

  def getToken(token: String): Token = {

    val id = idFromToken.get(token)

    if (id == 0)
      Token.UNKNOWN
    else
      new Token(id, token, counts(id))
  }

  def getTokenByID(id: Int): Token = {
    val token = tokenForId(id)
    val count = counts(id)
    new Token(id, token, count)
  }



}
