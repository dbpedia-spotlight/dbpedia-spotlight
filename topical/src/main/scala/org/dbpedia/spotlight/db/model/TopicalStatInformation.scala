package org.dbpedia.spotlight.db.model

import io.Source
import java.io._
import scala.collection.mutable._
import collection.mutable

/**
 * Class that handles word count information produced during corpus generation.
 */
object TopicalStatInformation {
  def fromLine(line: String): (Topic, Double, Map[String, (Double, Double)]) = {
    val scanner = new java.util.Scanner(line).useDelimiter(" |:")

    var words = Map[String, (Double, Double)]()
    val topicName = scanner.next()
    val docNumber = scanner.nextDouble()
    while (scanner.hasNext) {
      words += (scanner.next ->(scanner.nextDouble(), scanner.nextDouble()))
    }

    (new Topic(topicName), docNumber, words)
  }
}

class TopicalStatInformation(private val infoFile: File) {
  private val LOG = LogFactory.getLog(getClass)

  private var wordFrequencies = Map[Topic, Map[String, (Double, Double)]]()
  private var docCounts = Map[Topic, Double]()

  var loaded = false

  {
    if (infoFile != null)
      try {
        Source.fromFile(infoFile).getLines().foreach(thisLine => {
          val (topic, docCount, frequencies) = TopicalStatInformation.fromLine(thisLine)
          wordFrequencies += (topic -> frequencies)
          docCounts += (topic -> docCount)
        })
        loaded = true
        LOG.info("Topic information loaded")
      }
      catch {
        case e: FileNotFoundException => {
          LOG.info("New topic information created")
        }
        case e: IOException => {
          e.printStackTrace
        }
      }
  }

  /**
   *
   * @return word frequencies 1st:tf 2nd:document-frequency
   */
  def getWordFrequencies(topic: Topic) = wordFrequencies.getOrElse(topic, Map[String, (Double, Double)]())

  def setWordFrequencies(topic: Topic, newFrequencies: Map[String, (Double, Double)]) {
    wordFrequencies.update(topic, newFrequencies)
  }

  def getTF(topic: Topic, word: String) = getWordFrequencies(topic).getOrElse(word, (0.0, 0.0))._1

  def getDF(topic: Topic, word: String) = getWordFrequencies(topic).getOrElse(word, (0.0, 0.0))._2

  def putWordFrequency(topic: Topic, word: String, tf: Double, df: Double) {
    getWordFrequencies(topic) += (word ->(tf, df))
  }

  def contains(topic: Topic, word: String): Boolean = getWordFrequencies(topic).contains(word)

  def newTopic(topic: Topic, numDocs: Double = 0.0) {
    wordFrequencies += (topic -> Map[String, (Double, Double)]()); docCounts += (topic -> numDocs)
  }

  def getNumDocs(topic: Topic) = docCounts.getOrElse(topic, 0.0)

  def setNumDocs(topic: Topic, num: Double) = docCounts.update(topic, num)

  def incNumDocs(topic: Topic, num: Double) = docCounts(topic) += num

  def getTopics = wordFrequencies.keySet

  def persist {
    val pw: PrintWriter = new PrintWriter(new FileWriter(infoFile))

    wordFrequencies.foreach {
      case (topic, frequencies) => {
        pw.print(topic.getName + " " + docCounts(topic))
        if (frequencies != null)
          frequencies.foreach {
            case (word, (tf, df)) => pw.print(" " + word + ":" + tf + ":" + df)
          }
        pw.println()
        pw.flush()
      }
    }

    pw.close()
  }

}
