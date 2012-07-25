package org.dbpedia.spotlight.topic.wikipedia.util

import io.Source
import org.apache.commons.logging.LogFactory
import scala.collection.mutable.{Map, Set}
import org.dbpedia.spotlight.model.DBpediaCategory

/**
 * Utility object which loads dbpedias hierarchy from skos_categories into memory.
 *
 * @author Dirk Weißenborn
 */
object WikipediaHierarchyLoader {

  private val LOG = LogFactory.getLog(getClass)

  def loadCategoryHierarchy(pathToWikiCategories: String): Map[DBpediaCategory, Set[DBpediaCategory]] = {
    LOG.info("Loading wikipedia hierarchy")
    var result: Map[DBpediaCategory, Set[DBpediaCategory]] = Map()

    Source.fromFile(pathToWikiCategories).getLines().foreach(line => {
      val split = line.split(" ")
      if (split.length >= 3 && split(1).equals("<http://www.w3.org/2004/02/skos/core#broader>")) {
        val category = new DBpediaCategory(split(0).replace("<", "").replace(">", ""))
        val parent = new DBpediaCategory(split(2).replace("<", "").replace(">", ""))
        if (!result.contains(parent))
          result += (parent -> Set())
        if (!result.contains(category))
          result += (category -> Set())

        result(parent) += (category)
      }
    })

    result
  }

}
