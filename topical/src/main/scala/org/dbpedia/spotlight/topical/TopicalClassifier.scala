package org.dbpedia.spotlight.topical

import org.dbpedia.spotlight.model.{Text, Topic}
import org.dbpedia.spotlight.log.SpotlightLog
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
    def trainModelIncremental(corpus:File, iterations:Int, classifier:TopicalClassifier)

    def trainModel(corpus:Iterator[(Topic,Text)],  iterations:Int):TopicalClassifier
    def trainModelIncremental(corpus:Iterator[(Topic,Text)],  iterations:Int, classifier:TopicalClassifier)

    def trainModel(corpus:File):TopicalClassifier = trainModel(corpus,1)
    def trainModelIncremental(corpus:File,classifier:TopicalClassifier) {
        trainModelIncremental(corpus,1,classifier)
    }

    def trainModel(corpus:Iterator[(Topic,Text)]):TopicalClassifier = trainModel(corpus,1)
    def trainModelIncremental(corpus:Iterator[(Topic,Text)],classifier:TopicalClassifier) {
        trainModelIncremental(corpus,1,classifier)
    }

    def needsShuffled:Boolean = false


    def main(args:Array[String]) {
        if(args.size < 2) {
            throw new IllegalArgumentException("You have to provide at least: path to corpus, model output path [, optional:iterations]")
        }

        val iterations = if(args.length>2) args(2).toInt else 1

        val m1 = trainModel(new File(args(0)),iterations)
        m1.serialize(new File(args(1)))
    }
}

object TopicalClassifierTrainer {
    def byType(classifierType:String):TopicalClassifierTrainer =
        classifierType match {
            case _ => NaiveBayesTopicalClassifier // default
        }
}

object TopicalClassifierFactory {
    def fromFile(file:File, classifierType: String): Option[TopicalClassifier] = {
        SpotlightLog.info(this.getClass, "Loading topical classifier...")

        if (classifierType.endsWith("NaiveBayesTopicalClassifier")) {
            return Some(NaiveBayesTopicalClassifier.deSerialize(file))
        }

        None
    }

    def fromType(classifierType:String): Option[TopicalClassifier] = {
        if (classifierType.endsWith("NaiveBayesTopicalClassifier")) {
            val classifier = new NaiveBayesTopicalClassifier
            return Some(classifier)
        }

        None
    }
}