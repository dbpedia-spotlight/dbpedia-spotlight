package org.dbpedia.spotlight.model

/**
 * @author Joachim Daiber
 */

class Token(val token: String, val offset: Int, var tokenType: TokenType) extends HasFeatures {

  override def hashCode() = "%s, %d".format(token, offset).hashCode

  override def equals(obj: Any): Boolean = {
    obj match {
      case token: Token => token.equals(token.token)
      case _ => false
    }
  }

  override def toString = "%s (ID: %d, count: %s)".format(token, tokenType.id, if(tokenType != null) tokenType.count.toString else "unknown")

}