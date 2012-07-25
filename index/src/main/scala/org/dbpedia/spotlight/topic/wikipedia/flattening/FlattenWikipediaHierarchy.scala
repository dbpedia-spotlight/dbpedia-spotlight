package org.dbpedia.spotlight.topic.wikipedia.flattening

import scala.collection.mutable.{Map, Set}
import io._
import org.apache.commons.logging.LogFactory
import actors.Actor
import actors.Future
import java.io.{File, FileWriter, PrintWriter}
import org.dbpedia.spotlight.topic.wikipedia.util.WikipediaHierarchyLoader
import org.dbpedia.spotlight.model.{DBpediaCategory, Topic}
import org.dbpedia.spotlight.util.IndexingConfiguration

/**
 * Object which iterates over the whole {@link http://downloads.dbpedia.org/3.7/en/skos_categories_en.nt.bz2 wikipedia_hierarchy}
 * (extracted by dbpedia) and flattens it to the topics, specified in the topics file which should have the following format: <br/>
 * topicLabel=wikicategory1,wikicategory2,...
 *
 * @deprecated rather use FlattenHierarchyByTopics
 * @author dirk
 */
object FlattenWikipediaHierarchy {

  private val LOG = LogFactory.getLog(this.getClass)
  private var deepest = 0

  /**
   *
   * @param args 1st: indexing.properties 2nd:maximal depth (maximal distance of specified topic categories to categories that will be assigned), 3rd: (optional) path to topics
   */
  def main(args: Array[String]) {
    val config = new IndexingConfiguration(args(0))

    if (args.size > 2)
      flattenHierarchy(args(2), args(1).toInt,
                       config.get("org.dbpedia.spotlight.data.categories"),
                       config.get("org.dbpedia.spotlight.topic.flattenedHierarchy"))
    else
      LOG.error("no topic file specified")

    System.exit(0)
  }

  def flattenHierarchy(pathToTopics: String, maxDepth: Int, pathToWikiCategories: String, outputPath: String) {
    var topics = Map[Topic, List[DBpediaCategory]]()
    var topCats = List[DBpediaCategory]()
    val source = Source.fromFile(pathToTopics)

    source.getLines().foreach(topic => {
      val split = topic.split("=")
      var list = List[DBpediaCategory]()
      split(1).split(",").foreach(category => list = new DBpediaCategory(category.trim) :: list)
      topics += (new Topic(split(0)) -> list)
      topCats ++= list
    })

    flattenHierarchyToCategoriesParallel(topCats, pathToWikiCategories, outputPath)

    var others = Set[(DBpediaCategory, Int)]()

    topics.foreach { case (topic,categories) => {

      LOG.info("Merging files to topic " + topic.getName)
      val writer = new PrintWriter(new FileWriter(outputPath + "/" + topic.getName + ".tsv"))
      val readers = new Array[Iterator[String]](categories.size)
      val lines = new Array[String](categories.size)
      var depth = 0
      var split: Array[String] = null

      for (i <- 0 until categories.size) {
        readers(i) = Source.fromFile(outputPath + "/" + categories(i).getCategory + ".tsv.writing").getLines()
        if (readers(i).hasNext)
          lines(i) = readers(i).next()
        else
          lines(i) = ""
      }

      while (depth <= maxDepth) {
        for (i <- 0 until categories.size) {
          split = if (lines(i) == "") null else lines(i).split("\t")

          while (split != null && split(1).toInt <= depth) {
            writer.println(lines(i))
            if (readers(i).hasNext) {
              lines(i) = readers(i).next()
              split = lines(i).split("\t")
            }
            else {
              lines(i) = ""
              split = null
            }
          }
        }
        depth += 1
      }

      writer.close()

      for (i <- 0 until categories.size) {
        while (lines(i) != "") {
          split = lines(i).split("\t")
          others += ((new DBpediaCategory(split(0)), deepest - split(1).toInt - 1))

          if (readers(i).hasNext)
            lines(i) = readers(i).next()
          else
            lines(i) = ""
        }
      }


      for (i <- 0 until categories.size) {
        new File(outputPath + "/" + categories(i).getCategory + ".tsv.writing").delete()
      }

    }}

    val sortedOthers = others.toList.sortBy(entry => entry._2)
    val writer = new PrintWriter(new FileWriter(outputPath + "/others.tsv"))

    sortedOthers.foreach(other => writer.println(other._1.getCategory + "\t" + other._2))

    writer.close()

  }

  def flattenHierarchyToCategoriesParallel(topCats: List[DBpediaCategory], pathToWikiCategories: String, outputPath: String) {
    val hierarchy = WikipediaHierarchyLoader.loadCategoryHierarchy(pathToWikiCategories)
    val sortedCats = topCats.sortBy(cat => cat.getCategory)
    val iterators = new Array[QueueIterator](sortedCats.size)

    for (i <- 0 until sortedCats.size) {
      iterators(i) = new QueueIterator(sortedCats(i), hierarchy, outputPath)
    }
    val responses: Array[Future[Any]] = new Array(sortedCats.size)

    var done = false
    while (!done) {
      LOG.info("Processing depth " + deepest)
      done = true
      for (i <- 0 until sortedCats.size) {
        iterators(i).start()
        responses(i) = (iterators(i) !! "go")
      }
      for (i <- 0 until sortedCats.size)
        responses(i)() match {
          case b: Boolean => done = done && b
        }
      LOG.info("Writing results for depth " + deepest)
      iterators.foreach(it => {
        it.writeResult
        it.toRemove.foreach(element => hierarchy.remove(element))
        it.toRemove = Set()
      })
      deepest += 1
    }

    iterators.foreach(it => {
      it.start()
      it !! "STOP"
    })
  }

  class QueueIterator(topCategory: DBpediaCategory,
                      hierarchy: Map[DBpediaCategory, Set[DBpediaCategory]],
                      outputPath: String) extends Actor {

    var result: Map[DBpediaCategory, Int] = Map[DBpediaCategory, Int]()
    var queue = Set[DBpediaCategory](topCategory)
    var ctr = 0
    var toRemove: Set[DBpediaCategory] = Set()
    val size = hierarchy.size

    def act() {
      Actor.loop {
        react {
          case "go" => {
            if (!done) {
              var newQueue = Set[DBpediaCategory]()
              queue.foreach(category => {
                val subcategories = hierarchy.getOrElse(category, null)
                if (subcategories != null) {
                  toRemove += (category)

                  result += (category -> ctr)
                  newQueue ++= subcategories
                }
              })
              queue = newQueue
            }
            ctr += 1
            reply(done)
          }
          case _ => exit()
        }
      }
    }

    def done: Boolean = queue.isEmpty

    def writeResult {
      val outputWriter = new PrintWriter(new FileWriter(outputPath + "/" + topCategory + ".tsv.writing", true))

      result.foreach(cat => outputWriter.println(cat._1.getCategory + "\t" + cat._2))

      result.clear()
      outputWriter.close()
    }

  }

}
