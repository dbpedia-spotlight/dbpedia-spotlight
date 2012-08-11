package org.dbpedia.spotlight.topic

import scala.collection.mutable.Map
import scala.collection.mutable.Set
import java.io.{FileWriter, PrintWriter, File}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.io.FileOccsCategoriesSource
import org.dbpedia.spotlight.model.{Topic, DBpediaCategory}
import org.dbpedia.spotlight.util.IndexingConfiguration
import util.TopicUtil
import wikipedia.util.WikipediaFlattenedHierarchyLoader


/**
 * This object takes the sorted occs file extracted by ExtractOccsFromWikipedia, a flattened hierarchy (created by either
 * FlattenHierarchyByClusters, FlattenHierarchySemiSupervised or FlattenWikipediaHierarchy)
 * and dbpedias sorted article_categories file (http://downloads.dbpedia.org/3.7/en/article_categories_en.nt.bz2) and
 * splits the occs file into files foreach topic
 *
 * @author dirk
 */
//TODO just allow concept uris
object SplitOccsByCategories {

  private val LOG = LogFactory.getLog(getClass)

  /**
   *
   * @param args 1st: indexing.properties 2nd: path to sorted occs file, 3rd: path to output directory
   *
   */
  def main(args: Array[String]) {
    if (args.length > 2) {
      val config = new IndexingConfiguration(args(0))
      splitOccs(config.get("org.dbpedia.spotlight.topic.flattenedHierarchy"),
        config.get("org.dbpedia.spotlight.data.sortedArticlesCategories"),
        args(1), args(2))
    }
    else
      LOG.error("Not sufficient arguments!")
  }

  def splitOccs(pathToFlattenedHierarchy: String, pathToSortedArticlesCategories: String, pathToSortedOccsFile: String, outputPath: String) {
    val flattenedHierarchy = WikipediaFlattenedHierarchyLoader.loadFlattenedHierarchy(new File(pathToFlattenedHierarchy))

    new File(outputPath).mkdirs()
    val writers = Map[Topic, PrintWriter]()
    flattenedHierarchy.foreach {
      case (topic, categories) => {
        writers += (topic ->
          new PrintWriter(new FileWriter(outputPath + "/" + topic.getName + ".tsv")))
      }
    }
    writers += (TopicUtil.CATCH_TOPIC -> new PrintWriter(new FileWriter(outputPath + "/"+TopicUtil.CATCH_TOPIC.getName+".tsv")))

    var topics = Map[Topic, Double]()
    var oldCategories: Set[DBpediaCategory] = null
    var counter = 0
    var max = 0.0
    var distance = 0.0

    FileOccsCategoriesSource.fromFile(new File(pathToSortedOccsFile), new File(pathToSortedArticlesCategories)).foreach {
      case (resourceOcc, categories) => {
        counter += 1

        if (!categories.equals(oldCategories)) {
          topics = Map()
        }

        if (topics.isEmpty) {
          categories.foreach(category => {
            flattenedHierarchy.foreach {
              case (topic, subcategories) => {
                distance = subcategories.getOrElse(category, -1.0)
                if (distance > 0.0) {
                  if (topics.contains(topic))
                    topics(topic) += 1 / distance
                  else
                    topics += (topic -> 1.0 / distance)
                }
              }
            }
          })
        }

        if (topics.isEmpty)
          writers(TopicUtil.CATCH_TOPIC).println(resourceOcc.toTsvString)
        else {
          max = topics.maxBy(_._2)._2
          topics = Map() ++ topics.filter(element => element._2 >= max)
          topics.foreach(topic => writers(topic._1).println(resourceOcc.toTsvString))
        }

        if (counter % 100000 == 0)
          LOG.info(counter + " occs processed")

        oldCategories = categories
      }

    }

    writers.foreach(writer => writer._2.close())
  }

}
