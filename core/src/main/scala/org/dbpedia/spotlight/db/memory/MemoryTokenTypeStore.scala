package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.db.model.TokenTypeStore
import org.dbpedia.spotlight.model.TokenType
import util.StringToIDMapFactory

/**
 * A memory-based store for
 *
 * @author Joachim Daiber
 */

@SerialVersionUID(1006001)
class MemoryTokenTypeStore
  extends MemoryStore
  with TokenTypeStore
{

  var tokenForId: Array[String] = null
  var counts: Array[Int] = null

  @transient
  var idFromToken: java.util.Map[String, java.lang.Integer] = null

  @transient
  var totalTokenCount: Double = 0.0

  @transient
  var vocabularySize: Int = 0


  override def loaded() {
    counts.foreach( c =>
      totalTokenCount += c
    )
    vocabularySize = counts.size
    createReverseLookup()
  }

  def size = tokenForId.size

  def createReverseLookup() {
    if (tokenForId != null) {
      SpotlightLog.info(this.getClass, "Creating reverse-lookup for Tokens.")
      idFromToken = StringToIDMapFactory.createDefault(tokenForId.size)

      var id = 0
      tokenForId foreach { token => {
        idFromToken.put(token, id)
        id += 1
       }
      }
    }
  }

  def getTokenType(token: String): TokenType = {

    val id = idFromToken.get(token)

    if (id == null)
      TokenType.UNKNOWN
    else
      new TokenType(id, token, counts(id))
  }

  def getTokenTypeByID(id: Int): TokenType = {
    id match {
      case TokenType.UNKNOWN.id => TokenType.UNKNOWN
      case TokenType.STOPWORD.id => TokenType.STOPWORD
      case regularId =>
        val token = tokenForId(regularId)
        val count = counts(regularId)
        new TokenType(regularId, token, count)
    }
  }

  def getTotalTokenCount: Double = totalTokenCount

  def getVocabularySize: Int = vocabularySize

}
