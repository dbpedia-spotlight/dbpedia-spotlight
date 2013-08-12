package org.dbpedia.spotlight.model

import org.dbpedia.extraction.util.WikiUtil

/**
 * String wrapper class for dbpedia categories
 * @param category
 *
 * @author dirk
 */
class DBpediaCategory(private var category: String) {

    category = category.replace(DBpediaCategory.DBPEDIA_CATEGORY_PREFIX, "")

    category = if (isEncoded(category)) category else WikiUtil.wikiEncode(category)

    //CAREFUL: Copied from DBpediaResource
    private def isEncoded(s: String) = """%[0-9a-fA-F][0-9a-fA-F]""".r.findFirstIn(s) != None


    def getFullUri = DBpediaCategory.DBPEDIA_CATEGORY_PREFIX + category

    def getCategory = category

    override def equals(that: Any) = {
        that match {
            case t: DBpediaCategory => this.category.equals(t.category)
            case _ => false
        }
    }

    override def hashCode(): Int = {
        (if (category != null) category.hashCode else 0)
    }

    override def toString(): String = category.toString
}

object DBpediaCategory {
    val DBPEDIA_CATEGORY_PREFIX = "http://dbpedia.org/resource/Category:"
}
