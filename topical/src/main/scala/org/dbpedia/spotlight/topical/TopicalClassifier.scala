package org.dbpedia.spotlight.topical

import scala.collection.mutable._
import org.dbpedia.spotlight.model.{TopicalClassificationConfiguration, Text, Topic}
import org.apache.commons.logging.LogFactory


/**
 * A classifier that can predict the topic of a given context.
 */
trait TopicalClassifier {

    /**
     * @param text
     * @return predicted probabilities of topics given the text
     */
    def getPredictions(text: Text): Array[(Topic, Double)]

    /**
     * @param ids
     * @param values
     * @return predicted probabilities of topics given a word vector (ids come from a dictionary that translates words to ids)
     */
    def getPredictions(ids: Array[Int], values: Array[Double]): Array[(Topic, Double)]

    /**
     * @return list of topics the classifier is able to predict
     */
    def getTopics(): List[Topic]

    /**
     * Trains the model on this text.
     * @param text
     * @param topic
     * @param increaseVocabulary >0, if dictionary should be increased for this text by specified number of words.
     */
    def update(text: Text, topic: Topic, increaseVocabulary: Int = 0)

    /**
     * Trains the model with the specified id->topic vector
     * @param vector
     * @param topic
     */
    def update(vector: Map[Int, Double], topic: Topic)

    def persist
}

trait MultiLabelClassifier extends TopicalClassifier {
    protected val NEGATIVE_TOPIC_PREFIX = "_"

    /**
     * Trains  the model with this text as a negative example for the topic
     * @param text
     * @param topic
     * @param increaseVocabulary
     */
    def updateNegative(text: Text, topic: Topic, increaseVocabulary: Int = 0)

    /**
     * Trains  the model with this vector as a negative example for the topic
     * @param vector
     * @param topic
     */
    def updateNegative(vector: Map[Int, Double], topic: Topic)
}

object TopicalClassifier {
    private val LOG = LogFactory.getLog(getClass)

    def fromConfig(config: TopicalClassificationConfiguration): WekaMultiLabelClassifier = {
        LOG.info("Loading topical classifier...")
        val info = config.loadTopicInfo
        val dic = config.loadDictionary
        if (config.getClassifierType == "org.dbpedia.spotlight.topic.WekaSingleLabelClassifier") {
            new WekaSingleLabelClassifier(dic, info, config.getModelFile, null, info.loaded)
        }
        if (config.getClassifierType == "org.dbpedia.spotlight.topic.WekaMultiLabelClassifier")
            new WekaMultiLabelClassifier(dic, info, config.getModelFile, info.loaded)

        null
    }
}