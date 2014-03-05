package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.TokenType


/**
 * A store interface for token types.
 *
 * A Token object in a Text is a specific instance of a TokenType. A TokenType can be the stemmed,
 * lower-cased or otherwise processed.
 *
 * @author Joachim Daiber
 */

trait TokenTypeStore {

  /**
   * Returns ID and count of a token in the database.
   * @param tokenType the token type
   * @return
   */
  def getTokenType(tokenType: String): TokenType

  /**
   * Returns the name and count for a token.
   *
   * @param id internal ID of the TokenType object
   * @return
   */
  def getTokenTypeByID(id: Int): TokenType

  /**
   * Returns the total count of all tokens in the database.
   *
   * @return
   */
  def getTotalTokenCount: Double

  /**
   * Returns the number of token types (distinct tokens) in the database.
   *
   * @return
   */
  def getVocabularySize: Int

}
