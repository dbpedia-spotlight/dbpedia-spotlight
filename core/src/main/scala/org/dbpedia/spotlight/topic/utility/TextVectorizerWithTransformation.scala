package org.dbpedia.spotlight.topic.utility

import scala.collection.mutable._
import org.dbpedia.spotlight.model.Topic

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 5/30/12
 * Time: 2:49 PM
 * To change this template use File | Settings | File Templates.
 */
//TODO merge with TextVectorizer or rename
protected[topic] class TextVectorizerWithTransformation(private val dictionary: WordIdDictionary,private val topicInfo: TopicalStatInformation) {

  private val vectorizer = new TextVectorizer(dictionary.isPhonetic)

  def textToTranformedInput(text: String, transform: Boolean = true, isTraining:Boolean = false, topic:Topic = null, increaseVocabulary:Int = 0): Map[Int, Double] = {
    var vector = vectorizer.getWordCountVector(text)

    //if there should be no transformation, keeping stats is not important
    if (isTraining) {
      if (transform) {
        topicInfo.incNumDocs(TopicUtil.OVERALL_TOPIC, 1)
        topicInfo.incNumDocs(topic, 1)
      }

      if (increaseVocabulary > 0)
        vector.filter(entry => dictionary.getId(entry._1) < 0).toList.sortBy(-_._2).
          take(math.min(increaseVocabulary, dictionary.getSpace)).foreach {
          case(word,_) => {
            dictionary.put(word, dictionary.getSize)
            if (transform) {
              topicInfo.putWordFrequency(TopicUtil.OVERALL_TOPIC, word, 0.0, 0.0)
              topicInfo.putWordFrequency(topic, word, 0.0, 0.0)
            }
          }
        }
    }

    vector = Map() ++ vector.filter(entry => dictionary.getId(entry._1) > 0)

    if (isTraining && transform) {
      val sum = math.sqrt(vector.foldLeft(0.0)((ctr, element)=>ctr + element._2*element._2))

      vector.foreach{ case (word,count) => {
        var stats = topicInfo.getWordFrequencies(topic)(word)
        topicInfo.putWordFrequency(topic, word, stats._1 + count/sum, stats._2 + 1 )

        stats = topicInfo.getWordFrequencies(TopicUtil.OVERALL_TOPIC)(word)
        topicInfo.putWordFrequency(TopicUtil.OVERALL_TOPIC, word, stats._1 + count/sum, stats._2 + 1)
      } }
    }

    if (transform) {
      val docSum: Double = topicInfo.getNumDocs(TopicUtil.OVERALL_TOPIC)

      //TF-IDF
      vector = Map() ++ vector.transform((word, value) => math.sqrt(value)* math.log(docSum / topicInfo.getDF(TopicUtil.OVERALL_TOPIC, word)) / math.log(2))
    }

    //Length normalization
    var squaredSum = vector.foldLeft(0.0)((acc, entry) => acc + entry._2 * entry._2)
    squaredSum = math.sqrt(squaredSum)

    var idVector = Map[Int, Double]()
    vector.foreach{ case (word,count) => {

        val id = dictionary.getId(word)
      if (id>=0)
        idVector += (dictionary.getId(word) -> count / squaredSum)
    } }


    idVector
  }
}
