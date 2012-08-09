package org.dbpedia.spotlight.topic.wikipedia.flattening

import java.io.{FileWriter, PrintWriter, File}
import org.apache.commons.logging.LogFactory
import scala.collection.mutable._
import org.dbpedia.spotlight.io.FileOccsCategoriesSource
import actors.Actor
import java.util.regex.Pattern
import java.util.Scanner
import org.dbpedia.spotlight.util.{TextVectorizer, IndexingConfiguration}
import org.dbpedia.spotlight.db.model.WordIdDictionary
import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, DBpediaCategory}


/**
 * a) Writes splitted vectors (that have to be sorted and merged) to file <br/>
 * args: "extract", sorted-occs-file, output-file, offset (from which occ to start), indexing.properties-file
 * offset is the number of occs to skip in occs file
 * <br/>
 * b) Merge sorted splitted vectors back together mergeSortedSplittedVectors  <br/>
 * args: "merge"   sorted-vectors   output-file   number-of-categories-to-keep   indexing.properties-file
 */
//TODO just allow concept uris
object ExtractCategoryCorpus {

  private val LOG = LogFactory.getLog(getClass)

  def main(args: Array[String]) {
    val config = new IndexingConfiguration(args(4))

    if (args.length < 3) {
      System.exit(-1)
    }

    if (args(0) == "extract") {
      val producer = new Producer(FileOccsCategoriesSource.fromFile(new File(args(1)), new File(config.get("org.dbpedia.spotlight.data.sortedArticlesCategories"))), args(3).toInt)
      val consumer = new Consumer(producer, args(2))
    }

    if (args(0) == "merge") {
      mergeSortedSplittedVectors(args(1), args(2), args(3).toInt,config.get("org.dbpedia.spotlight.topic.cluster.dictionary"), args(5).toDouble)
    }
  }

  /**
   * Merges splittet vectors written by this class (from call 'extract') back together after sorting the output file and
   * applies to each vector tf/idf transformation and cuts
   * @param input file of sorted splitted vectors produced by 'ectract'
   * @param outputPath output path
   * @param maxNrOfCats number of categories to keep (keep those with the greatest wordvectors, because some categories are really sparse)
   * @param pathToDictionary path to word->id dictionary
   * @param normalization scaling factor (new word count = old count / factor)
   */
  def mergeSortedSplittedVectors(input: String, outputPath: String, maxNrOfCats: Int, pathToDictionary:String,normalization:Double) {
    var frequencies = Map[String, Array[Double]]()

    var categoryContentSize = Map[String, Double]()
    var categorySize: Double = 0.0

    LOG.info("collecting statistics")
    var counter = 0

    var category = ""
    var oldCategory = ""

    var wordVector: Map[String, Double] = null

    var docsum = 0
    var writer = new PrintWriter(new FileWriter(outputPath + ".writing"))
    var word: Array[String] = null
    var value = 0.0
    val patternMatcher = Pattern.compile("[a-z]{3,}").matcher("")
    val doublePatternMatcher = Pattern.compile("[0-9]+\\.[0-9]+").matcher("")
    var tempFreq: List[(String, Array[Double])] = null

    //using scanner because size of lines is huge
    var scanner = new Scanner(new File(input))
    scanner.useDelimiter("\\s")

    if (scanner.hasNext)
      word = Array(scanner.next())

    while (scanner.hasNext) {
      category = word.reduceLeft(_ + ":" + _)

      if (!category.equals(oldCategory)) {
        if (wordVector != null) {
          docsum += 1
          if (docsum % 1000 == 0)
            LOG.info(docsum + " categories processed!")
          categorySize = 0

          writer.print(oldCategory)
          wordVector.foreach(word => {
            writer.print("\t" + word._1 + ":" + word._2)
            categorySize += word._2
          })
          categoryContentSize += (oldCategory -> categorySize)
          writer.println()
          wordVector = null
        }
        wordVector = Map()
      }
      oldCategory = category

      while (scanner.hasNext && {
        word = scanner.next().split(":"); word.size > 1
      } && {
        doublePatternMatcher.reset(word.last); doublePatternMatcher.matches()
      }) {
        value = word.last.toDouble
        patternMatcher.reset(word(0))
        if (patternMatcher.matches()) {
          if (frequencies.contains(word(0))) {
            frequencies(word(0))(0) += value
            if (!category.equals(oldCategory))
              frequencies(word(0))(1) += 1
          }
          else
            frequencies += (word(0) -> Array(value, 1))

          if (wordVector.contains(word(0)))
            wordVector(word(0)) += value
          else
            wordVector += (word(0) -> value)
        }
      }

      if (frequencies.size > 500000) {
        counter = 0
        tempFreq = frequencies.toList.sortBy((entry) => -math.sqrt(entry._2(0)) * math.sqrt(docsum / entry._2(1)))
        frequencies = null
        frequencies = Map()
        tempFreq.takeWhile(word => {
          frequencies += word
          counter += 1
          counter < 250000
        })
        tempFreq = null
      }
    }
    wordVector = null
    writer.close()

    val dictionary = new WordIdDictionary(pathToDictionary,100000)

    val clone: Map[String, Array[Double]] = frequencies.clone()
    val bound = 100000
    if (bound > dictionary.getDictionary.size) {
      LOG.info("calculate dictionary entries")

      val sortedWordvectors = clone.toList.sortBy((entry) => -math.sqrt(entry._2(0)) * math.log(docsum / entry._2(1)))

      for (i <- 0 until math.min(bound, clone.size))
        dictionary.put(sortedWordvectors(i)._1, i)
    }
    dictionary.persist()

    val keptCats: Set[String] = Set() ++ categoryContentSize.toList.sortBy(entry => -entry._2).take(maxNrOfCats).map[String, List[String]](entry => entry._1).toSet

    counter = 0

    //write input
    writer = new PrintWriter(new FileWriter(outputPath))
    val catWriter = new PrintWriter(new FileWriter(new File(outputPath).getParentFile.getAbsolutePath + "/categories.list"))

    val restWriter = new PrintWriter(new FileWriter(outputPath+".rest"))
    val restCatWriter = new PrintWriter(new FileWriter(new File(outputPath).getParentFile.getAbsolutePath + "/categories.list.rest"))

    var wordId = -1
    var values: List[(Int, Double)] = List()
    var iterator: Iterator[(Int, Double)] = null
    var element: (Int, Double) = null

    scanner = new Scanner(new File(outputPath + ".writing"))
    scanner.useDelimiter("\\s")

    var kept = true

    if (scanner.hasNext)
      word = Array(scanner.next())
    while (scanner.hasNext) {
      category = word.reduceLeft(_ + ":" + _)
      values = List()

      kept = keptCats.contains(category)
      while (scanner.hasNext && {
        word = scanner.next().split(":"); word.size > 1
      } && {
        doublePatternMatcher.reset(word.last); doublePatternMatcher.matches()
      }) {
          value = word.last.toDouble
          wordId = dictionary.getDictionary.getOrElse(word(0), -1)
          if (wordId > -1) {
            //IDF transformation
            values = (wordId, math.sqrt(docsum / frequencies(word(0))(1)) * value) :: values
          }
      }
      if (kept) {
        values = values.sortBy(_._1)
        writer.print("|")
        iterator = values.iterator
        while (iterator.hasNext) {
          element = iterator.next()
          if (element._2 > normalization/2)
            writer.print(" "+element._1 + ":" + (math.round(element._2/normalization)))
          if (!iterator.hasNext)
            writer.println()
        }
        catWriter.println(category)

        writer.flush()
        counter += 1
        if (counter % 10000 == 0)
          LOG.info(counter + " final examples written")
      }
      else {
        values = values.sortBy(_._1)
        restWriter.print("|")
        iterator = values.iterator
        while (iterator.hasNext) {
          element = iterator.next()
          if (element._2 > normalization/4)
            //*2 because of probable sparsity, because not kept categories are sparsely mentioned categories
            restWriter.print(" "+element._1 + ":" + (math.round(element._2/normalization*2)))
          if (!iterator.hasNext)
            restWriter.println()
        }
        restCatWriter.println(category)

        restWriter.flush()
        counter += 1
        if (counter % 10000 == 0)
          LOG.info(counter + " final examples written")
      }
    }

    writer.close()
    catWriter.close()
    restWriter.close()
    restCatWriter.close()

    new File(outputPath + ".writing").delete()
  }


  case class Produce(count: Int)

  case object Finished


  private class Producer(source: Traversable[(DBpediaResourceOccurrence, Set[DBpediaCategory])], offset: Int) extends Actor {
    def act() {
      react {
        case Produce(n: Int) => produce(n)
      }
    }

    def produce(batchSize: Int) {
      var occCounter = 0
      var batchCounter = 0
      var categoryVectors = Map[String, Map[String, Double]]()
      var vector: Map[String, Double] = null
      var categoryVec: Map[String, Double] = null
      val vectorBuilder = new TextVectorizer()

     source.foreach { case (resourceOcc, categories) => {
       if(offset > occCounter) {
         occCounter += 1
         if (occCounter % 100000 == 0)
           LOG.info(occCounter + " occs skipped")
       }
       else
         if(categoryVectors.size < batchSize) {
            val occText = resourceOcc.context
            if (occText != null) {
              vector = vectorBuilder.getWordCountVector(occText.text)

              categories.foreach(category => {
                // compute category vectors
                categoryVec = categoryVectors.getOrElseUpdate(category.getCategory, Map[String, Double]())
                for (word <- vector) {
                  if (categoryVec.contains(word._1))
                    categoryVec(word._1) += word._2
                  else
                    categoryVec += (word._1 -> word._2)
                }
              })
            }

            occCounter += 1
            if (occCounter % 100000 == 0)
              LOG.info(occCounter + " occs processed")
          }
          else {
            if (categoryVectors.size == 0)
              sender ! Finished
            else {
              batchCounter += 1
              LOG.info(batchCounter + " batches created")
              sender ! categoryVectors
            }
            categoryVectors = Map[String, Map[String, Double]]()
          }
      }  }
    }

    start
  }

  private class Consumer(producer: Producer, outputPath: String) extends Actor {
    new File(outputPath).getParentFile.mkdirs()
    var done = false
    val n = 30000
    var counter = 0

    def act() {
      requestWork()
      loopWhile(!done) {
        consume()
      }
    }

    def consume(): Nothing = react {
      case Finished => LOG.info("Finished"); done = true
      case categoryVectors: Map[String, Map[String, Double]] => work(categoryVectors)
    }

    def requestWork() {
      producer ! Produce(n)
    }

    def work(categoryVectors: Map[String, Map[String, Double]]) = {
      val outputWriter = new PrintWriter(new FileWriter(outputPath, true))
      categoryVectors.foreach(catVec => {
        outputWriter.print(catVec._1)
        catVec._2.foreach {
          case (wordId, wordCount) => outputWriter.print("\t" + wordId + ":" + wordCount)
        }
        outputWriter.println()
      })
      categoryVectors.keys.foreach(key => {
        categoryVectors(key) = null
      })

      outputWriter.close()
      counter += 1

      LOG.info(counter + " batches written")
    }

    start
  }

}
