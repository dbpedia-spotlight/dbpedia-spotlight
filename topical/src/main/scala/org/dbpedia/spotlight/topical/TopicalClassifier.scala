package org.dbpedia.spotlight.topical

import org.dbpedia.spotlight.model.{Text, Topic}
import org.apache.commons.logging.LogFactory
import java.io.File


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
     * @return list of topics the classifier is able to predict
     */
    def getTopics(): List[Topic]

    /**
     * Trains the model on this text.
     * @param text
     * @param topic
     */
    def update(text: Text, topic: Topic)


    def serialize(output:File)
}

trait MultiLabelClassifier extends TopicalClassifier {

    /**
     * Trains  the model with this text as a negative example for the topic
     * @param text
     * @param topic
     */
    def updateNegative(text: Text, topic: Topic)
}

object MultiLabelClassifier {
    final val NEGATIVE_TOPIC_PREFIX = "_"
}

trait TopicalClassifierTrainer {
    def trainModel(corpus:File, iterations:Int):TopicalClassifier

    def trainModel(corpus:Iterator[(Topic,Text)],  iterations:Int):TopicalClassifier

    def trainModel(corpus:File):TopicalClassifier = trainModel(corpus,1)

    def trainModel(corpus:Iterator[(Topic,Text)]):TopicalClassifier = trainModel(corpus,1)
}

object TopicalClassifierTrainer {
    def byType(classifierType:String):TopicalClassifierTrainer =
        classifierType match {
            case "FactorieTopicalClassifier" => FactorieTopicalClassifier
            case _ => NaiveBayesTopicalClassifier // default
        }

}

object TopicalClassifierFactory {
    private val LOG = LogFactory.getLog(getClass)

    def fromFile(file:File, classifierType: String): Option[TopicalClassifier] = {
        LOG.info("Loading topical classifier...")

        if (classifierType.endsWith("FactorieTopicalClassifier")) {
            return Some(FactorieTopicalClassifier.deSerialize(file))
        }

        if (classifierType.endsWith("NaiveBayesTopicalClassifier")) {
            return Some(NaiveBayesTopicalClassifier.deSerialize(file))
        }

        None
    }

    def fromType(classifierType:String): Option[TopicalClassifier] = {
        if (classifierType.endsWith("FactorieTopicalClassifier")) {
            val classifier = new FactorieTopicalClassifier
            return Some(classifier)
        }

        None
    }
}