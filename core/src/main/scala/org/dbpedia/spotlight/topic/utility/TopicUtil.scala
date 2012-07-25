package org.dbpedia.spotlight.topic.utility

import collection.mutable._
import java.io._
import io.Source
import org.dbpedia.spotlight.model.{Topic, DBpediaCategory}
import org.apache.commons.logging.LogFactory
import java.util

/**
 * Utility class which loads and persists topics and which can also load a WordIdDictionary
 */
object TopicUtil {
  private val LOG = LogFactory.getLog(getClass)

  val OVERALL_TOPIC = new Topic("_overall")
  val CATCH_TOPIC = new Topic("other")

  def getTopicInfo(pathToTopicInfo:String)= new TopicalStatInformation(pathToTopicInfo)

  def getDictionary(pathToDictionary: String, maxSize:Int): WordIdDictionary = new WordIdDictionary(pathToDictionary,maxSize)

}
