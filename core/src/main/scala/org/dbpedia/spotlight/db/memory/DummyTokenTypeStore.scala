package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.TokenTypeStore
import org.dbpedia.spotlight.model.TokenType
import sun.reflect.generics.reflectiveObjects.NotImplementedException

class DummyTokenTypeStore extends TokenTypeStore {

  /**
   * Returns ID and count of a token in the database.
   * @param tokenType the token type
   * @return
   */
  override def getTokenType(tokenType: String): TokenType = {
    new TokenType(0, tokenType, 0)
  }

  /**
   * Returns the name and count for a token.
   *
   * @param id internal ID of the TokenType object
   * @return
   */
  override def getTokenTypeByID(id: Int): TokenType = {
    throw new NotImplementedException
  }

  /**
   * Returns the number of token types (distinct tokens) in the database.
   *
   * @return
   */
  override def getVocabularySize: Int = 0

  /**
   * Returns the total count of all tokens in the database.
   *
   * @return
   */
  override def getTotalTokenCount: Double = 0.0
}
