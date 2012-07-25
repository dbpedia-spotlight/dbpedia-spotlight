package org.dbpedia.spotlight.topic.wikipedia.util

import org.dbpedia.spotlight.model.DBpediaTopic
import io.Source
import scala.collection.mutable._
import org.apache.commons.logging.LogFactory

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 6/22/12
 * Time: 2:33 PM
 * To change this template use File | Settings | File Templates.
 */

object WikipediaTopicVectorLoader {
  private val LOG = LogFactory.getLog(getClass)

  def loadTopicVectors(pathToVectors:String): (Map[DBpediaTopic, Map[String, Double]], Map[String, Double]) = {
    val topicVectors = Map[DBpediaTopic, Map[String, Double]]()
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
      topicVectors += (new DBpediaTopic(split(0)) -> words)
    })
    (topicVectors, idf)
  }

}
