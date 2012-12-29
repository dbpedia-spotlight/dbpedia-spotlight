package org.dbpedia.spotlight.topical.util

import org.dbpedia.spotlight.model.Topic
import java.io.File
import org.dbpedia.spotlight.db.model.{TopicalStatInformation, WordIdDictionary}

/**
 * Utility class which loads and persists topics and which can also load a WordIdDictionary
 */
object TopicUtil {
    val OVERALL_TOPIC = new Topic("_overall")
    val CATCH_TOPIC = new Topic("other")

    def getTopicInfo(pathToInfo: String) = new TopicalStatInformation(new File(pathToInfo))

    def getDictionary(pathToDic: String, maxSize: Int): WordIdDictionary = new WordIdDictionary(new File(pathToDic), maxSize)

}
