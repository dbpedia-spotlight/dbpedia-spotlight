package org.dbpedia.spotlight.model

import scala.Array
import scala.collection.mutable._
import org.dbpedia.spotlight.string.ModifiedWikiUtil

/**
 * String wrapper class for dbpedia categories
 * @param category
 *
 * @author dirk
 */
class DBpediaCategory(private var category: String) {

  category = category.replace(DBpediaCategory.DBPEDIA_CATEGORY_PREFIX, "")

  category = if (ModifiedWikiUtil.isEncoded(category)) {
                ModifiedWikiUtil.spaceToUnderscore(category).capitalize
              }
              else {
                ModifiedWikiUtil.wikiEncode(category)
              }

  def getFullUri = DBpediaCategory.DBPEDIA_CATEGORY_PREFIX + category

  def getCategory = category

  override def equals(that : Any) = {
    that match {
      case t: DBpediaCategory => this.category.equals(t.category)
      case _ => false
    }
  }

  override def hashCode() : Int = {
    (if (category != null) category.hashCode else 0)
  }

  override def toString() : String = category.toString
}

object DBpediaCategory {
  val DBPEDIA_CATEGORY_PREFIX = "http://dbpedia.org/resource/Category:"
}
