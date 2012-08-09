package org.dbpedia.spotlight.topic.util

import scala.collection.mutable._
import org.dbpedia.spotlight.model.Topic
import org.dbpedia.spotlight.db.model.{WordIdDictionary, TopicalStatInformation}
import org.dbpedia.spotlight.util.TextVectorizer

/**
 * This class converts textual input into word vectors applying transformations if needed. It also updates the dictionary
 * and topical information (which just keeps track of word occurrences within the topics) if it is desired, thus making this
 * class part of live training of the topical classifier.
 * @param dictionary
 * @param topicInfo
 */
//TODO better name!!
protected[topic] class TextVectorizerWithTransformation(private val dictionary: WordIdDictionary,private val topicInfo: TopicalStatInformation) {

  private val vectorizer = new TextVectorizer(dictionary.isPhonetic)

  def textToTranformedInput(text: String, transform: Boolean = true, isTraining:Boolean = false, topic:Topic = null, increaseVocabulary:Int = 0): Map[Int, Double] = {
    var vector = vectorizer.getWordCountVector(text)

    val _isTraining = isTraining && topicInfo.getTopics.contains(topic)

    //if there should be no transformation, keeping stats is not important
    if (_isTraining) {
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

    vector = Map() ++ vector.filter(entry => dictionary.getId(entry._1)>= 0)

    if (_isTraining && transform) {
      val sum = math.sqrt(vector.foldLeft(0.0)((ctr, element)=>ctr + element._2*element._2))

      vector.foreach{ case (word,count) => {
        var stats = topicInfo.getWordFrequencies(topic).getOrElse(word,(0.0,0.0))
        topicInfo.putWordFrequency(topic, word, stats._1 + count/sum, stats._2 + 1 )

        stats = topicInfo.getWordFrequencies(TopicUtil.OVERALL_TOPIC).getOrElse(word,(0.0,0.0))
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
        idVector += (id -> count / squaredSum)
    } }

    idVector
  }
}
