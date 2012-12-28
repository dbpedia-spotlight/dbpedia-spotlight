package org.dbpedia.spotlight.model

import scala.Int

class Topic(private val name: String) {
  def getName = name

  override def equals(that : Any) = {
    that match {
      case t: Topic => this.name.equals(t.name)
      case _ => false
    }
  }

  override def hashCode() : Int = {
    (if (name != null) name.hashCode else 0)
  }

  override def toString() : String = name
}
