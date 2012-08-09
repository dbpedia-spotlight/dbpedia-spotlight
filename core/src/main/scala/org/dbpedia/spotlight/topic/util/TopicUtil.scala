package org.dbpedia.spotlight.topic.util

import org.dbpedia.spotlight.model.Topic
import org.dbpedia.spotlight.db.model.{WordIdDictionary, TopicalStatInformation}

/**
 * Utility class which loads and persists topics and which can also load a WordIdDictionary
 */
object TopicUtil {
  val OVERALL_TOPIC = new Topic("_overall")
  val CATCH_TOPIC = new Topic("other")

  def getTopicInfo(pathToTopicInfo:String)= new TopicalStatInformation(pathToTopicInfo)

  def getDictionary(pathToDictionary: String, maxSize:Int): WordIdDictionary = new WordIdDictionary(pathToDictionary,maxSize)

}
