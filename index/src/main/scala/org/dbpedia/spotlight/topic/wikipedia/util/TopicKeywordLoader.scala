package org.dbpedia.spotlight.topic.wikipedia.util

import org.dbpedia.spotlight.model.Topic
import io.Source
import scala.collection.mutable._
import org.apache.commons.logging.LogFactory

/**
 * Utility object which loads keyword vectors from file (produced by TopicKeywordsFromWikipedia), and a corresponding
 * occurence count vector (number of topics where a word occurs, can be used for idf).
 */
object TopicKeywordLoader {
  private val LOG = LogFactory.getLog(getClass)

  def loadTopicVectors(pathToVectors:String): (Map[Topic, Map[String, Double]], Map[String, Double]) = {
    val topicVectors = Map[Topic, Map[String, Double]]()
    val idf = Map[String, Double]()
    Source.fromFile(pathToVectors).getLines().foreach(topicLine => {
      val split = topicLine.split(" ")
      val words = Map[String, Double]()
      var wordSplit: Array[String] = null
      split.drop(2).foreach(word => {
        try {
          wordSplit = word.split(":")
          words += (wordSplit(0) -> wordSplit(1).toDouble)
          if (!idf.contains(wordSplit(0)))
            idf += (wordSplit(0) -> 1)
          else
            idf(wordSplit(0)) += 1
        } catch {
          case ex: ArrayIndexOutOfBoundsException => LOG.warn("Found bad entry in topics definition file: " + word)
        }
      })
      topicVectors += (new Topic(split(0)) -> words)
    })
    (topicVectors, idf)
  }

}
