package org.dbpedia.spotlight.model

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class Token(val id: Int, val name: String, val count: Int) {

  override def hashCode() = name.hashCode

  override def equals(obj: Any): Boolean = {
    obj match {
      case token: Token => name.equals(token.name)
      case _ => false
    }
  }

  override def toString = "%s (%d)".format(name, count)

}

object Token {
  val UNKNOWN = new Token(0, "<<UNKNOWN>>", 1)
}