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
  def isStopWord(): Boolean = this.equals(TokenType.STOPWORD)

}

object TokenType {
  val UNKNOWN  = new TokenType(0, "<<UNKNOWN>>", 1)
  val STOPWORD = new TokenType(0, "<<STOPWORD>>", 1)
}
