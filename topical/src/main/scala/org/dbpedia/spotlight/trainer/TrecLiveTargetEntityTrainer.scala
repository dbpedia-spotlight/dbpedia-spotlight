package org.dbpedia.spotlight.trainer

import java.io.{FileWriter, PrintWriter, File}
import org.dbpedia.spotlight.feed.{FeedRegistry, FeedListener}
import org.dbpedia.spotlight.model._
import scala.collection.mutable.{Set, Map}
import org.apache.commons.logging.LogFactory

/**
 * Trainer of TrecTargetEntityClassifier, which consumes updates from feeds of type (Set[DBpediaResource], Text, Map[DBpediaResource,Int]), e.g. TrecResourceAnnotationFeed.
 *
 * @param modelDir
 * @param targetEntities
 * @param evalFile
 * @param evalInterval
 */
class TrecLiveTargetEntityTrainer(modelDir: File, targetEntities: Set[DBpediaResource] = null, evalFile: File = null, evalInterval: Int = 100) {
    private val LOG = LogFactory.getLog(getClass)

    private var evalWriter: PrintWriter = null
    if (evalFile != null)
        evalWriter = new PrintWriter(new FileWriter(evalFile, true))

    private val classifier = new TrecTargetEntityClassifier(modelDir, targetEntities)

    val feedListener = new FeedListener[(Set[DBpediaResource], Map[DBpediaResource, Double])]() {
        protected def update(item: (Set[DBpediaResource], Map[DBpediaResource, Double])) {
            train(item._1, item._2)
        }
    }

    private var updateCtr = 0
    private var meanSquaredError = 0.0

    def train(targets: Set[DBpediaResource], annotations: Map[DBpediaResource, Double]) {
        LOG.info("Updating target-entity classifier...")
        updateCtr += 1

        if (evalWriter != null) {
            val predictions = classifier.getPredictions(annotations)
            meanSquaredError += predictions.foldLeft(0.0)((sum, prediction) =>
                sum + math.pow({
                    if (targets.contains(prediction._1)) 1.0 else 0.0
                } - prediction._2, 2))

            if (updateCtr % evalInterval == 0) {
                evalWriter.println("===== Evaluation of interval " + (updateCtr - evalInterval) + " - " + updateCtr + " =====")
                evalWriter.println("mean squared error: " + (meanSquaredError / evalInterval))
                evalWriter.println("last predictions:" + predictions.toList.sortBy(-_._2).take(6).foldLeft("")(
                    (string, prediction) => string + " (" + prediction._2.toString.substring(0, math.min(4, prediction._2.toString.length)) + "," + prediction._1.uri + ")"))
                evalWriter.println("last targets:" + targets.foldLeft("")((string, target) => string + " " + target.uri))
                evalWriter.println("for annotations:" + annotations.foldLeft("")((string, annotation) => string + " (" + annotation._2 + "," + annotation._1.uri + ")"))
                evalWriter.flush
                meanSquaredError = 0.0
            }
        }

        classifier.update(targets, annotations)
    }

    def stopTraining {
        feedListener.unSubscribeToAllFeeds
        if (evalWriter != null)
            evalWriter.close()
    }

    def saveModel {
        classifier.persist
    }

    def subscribeToAll {
        feedListener.subscribeToAllFeeds
    }
}
