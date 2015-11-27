package org.dbpedia.spotlight.model

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class TokenType(val id: Int, val tokenType: String, val count: Int) {

  override def hashCode() = tokenType.hashCode

  override def equals(obj: Any): Boolean = {
    obj match {
      case token: TokenType => tokenType.equals(token.tokenType)
      case _ => false
    }
  }

  override def toString = "%s (%d)".format(tokenType, count)

}

object TokenType {
  val UNKNOWN  = new TokenType(-1, "<<UNKNOWN>>", 1)
  val STOPWORD = new TokenType(-2, "<<STOPWORD>>", 1)
}