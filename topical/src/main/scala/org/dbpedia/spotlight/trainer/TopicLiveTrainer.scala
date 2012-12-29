package org.dbpedia.spotlight.trainer

import org.dbpedia.spotlight.feed._
import org.dbpedia.spotlight.model._
import org.apache.commons.logging.LogFactory
import scala.collection.mutable._
import xml.XML
import java.io.{FileWriter, PrintWriter, File}
import org.dbpedia.spotlight.topical.{MultiLabelClassifier, TopicalClassifier}

/**
 * This class represents a trainer for a topical classifier. It basically consumes different Feeds that are topically
 * annotated, which are for now Feeds of type: (Topic, RssItem), (Set[Topic], Text), (Topic, Text). <br>
 * Training is done in the following manner: Calculate the topic predictions P of the model for the text X. If P for Topic T
 * is higher than minimal confidence and the update confidence for topic T is also higher than this minimal confidence,
 * than update the model with X as positive example for T. If however the update confidence for T is lower than minimal
 * confidence (which means that T is not assigned to X), than update the model with X as negative example for T.
 *
 * @param classifier classifier to update and save after training stopped
 * @param evalFile if defined, file which will contain training evaluations
 * @param evalInterval interval (measured in number of updates) between evaluations
 */
class TopicLiveTrainer(val classifier: TopicalClassifier, minimalConfidence: Double = 0.3, evalFile: File = null, evalInterval: Int = 100) {
    private val LOG = LogFactory.getLog(getClass)
    private var evalWriter: PrintWriter = null
    private var doEvaluation = false
    if (evalFile != null) {
        evalWriter = new PrintWriter(new FileWriter(evalFile, true))
        doEvaluation = true
    }

    private val rssFeedListener = new FeedListener[(Topic, RssItem)] {
        protected override def update(item: (Topic, RssItem)) {
            LOG.debug("Updating topical classifier from RSS...")
            updateModel(Map(item._1 -> 1), item._2.description)
        }
    }

    private val topicSetTextFeedListener = new FeedListener[(Map[Topic, Double], Text)] {
        protected override def update(item: (Map[Topic, Double], Text)) {
            LOG.debug("Updating topical classifier...")
            updateModel(item._1, item._2)
        }
    }

    private val topicTextFeedListener = new FeedListener[(Topic, Text)] {
        protected override def update(item: (Topic, Text)) {
            LOG.debug("Updating topical classifier...")
            updateModel(Map(item._1 -> 1), item._2)
        }
    }

    private var updateCtr = 0
    private var meanSquaredError = 0.0

    private def updateModel(topics: Map[Topic, Double], text: Text) {
        updateCtr += 1
        val predictions = classifier.getPredictions(text)

        if (doEvaluation) {
            meanSquaredError += predictions.foldLeft(0.0)((sum, prediction) =>
                sum + math.pow(topics.getOrElse(prediction._1, 0.0) - prediction._2, 2))

            if (updateCtr % evalInterval == 0) {
                evalWriter.println("===== Evaluation of interval " + (updateCtr - evalInterval) + " - " + updateCtr + " =====")
                evalWriter.println("mean squared error: " + (meanSquaredError / evalInterval))
                evalWriter.println("last topics: " + topics.toList.sortBy(-_._2).take(4).foldLeft("")(
                    (string, topic) => string + " (" + topic._2.toString.substring(0, math.min(4, topic._2.toString.length)) + "," + topic._1.getName + ")"))
                evalWriter.println("for text: " + text.text.substring(0, math.min(200, text.text.length)) + "...")
                evalWriter.println("last predictions:" + predictions.toList.sortBy(-_._2).take(4).foldLeft("")(
                    (string, prediction) => string + " (" + prediction._2.toString.substring(0, math.min(4, prediction._2.toString.length)) + "," + prediction._1.getName + ")"))
                evalWriter.flush()
                meanSquaredError = 0.0
                evalWriter.print("last trained topics: ")
            }
        }

        predictions.foreach {
            case (topic, prediction) =>
                if (prediction > minimalConfidence)
                    if (topics.getOrElse(topic, 0.0) < minimalConfidence) {
                        //this does nothing for a single label classifier
                        if (doEvaluation && updateCtr % evalInterval == 0)
                            evalWriter.print(" not_" + topic.getName)
                        if (classifier.isInstanceOf[MultiLabelClassifier])
                            classifier.asInstanceOf[MultiLabelClassifier].updateNegative(text, topic)
                    }
                    else {
                        if (doEvaluation && updateCtr % evalInterval == 0)
                            evalWriter.print(" " + topic.getName)
                        classifier.update(text, topic)
                    }
        }
        if (doEvaluation && updateCtr % evalInterval == 0)
            evalWriter.println
    }

    def subscribeToAll {
        rssFeedListener.subscribeToAllFeeds
        topicSetTextFeedListener.subscribeToAllFeeds
        topicTextFeedListener.subscribeToAllFeeds
    }

    def stopTraining {
        rssFeedListener.unSubscribeToAllFeeds
        topicSetTextFeedListener.unSubscribeToAllFeeds
        topicTextFeedListener.unSubscribeToAllFeeds
        if (evalWriter != null)
            evalWriter.close()
    }

    def saveModel {
        classifier.persist
    }
}
