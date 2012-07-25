package org.dbpedia.spotlight.live.trainer

import java.io.{FileWriter, PrintWriter, File}
import org.dbpedia.spotlight.model.{Topic, Text, DBpediaResource}
import org.dbpedia.spotlight.live.model.{RssItem, FeedRegistry, FeedListener}
import scala.collection.mutable.{Set,Map}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.live.model.trec.TrecTargetEntityClassifier

/**
 * Trainer of TrecTargetEntityClassifier, which consumes updates from feeds of type (Set[DBpediaResource], Text, Map[DBpediaResource,Int]), e.g. TrecResourceAnnotationFeed.
 *
 * @param modelDir
 * @param targetEntities
 * @param evalFile
 * @param evalInterval
 */
class TrecLiveTargetEntityTrainer(modelDir:File, targetEntities : Set[DBpediaResource] = null, evalFile:File = null, evalInterval:Int = 10) {
  private val LOG = LogFactory.getLog(getClass)

  private var evalWriter:PrintWriter = null
  if (evalFile!=null)
    evalWriter = new PrintWriter(new FileWriter(evalFile,true))

  private val classifier = new TrecTargetEntityClassifier(modelDir, targetEntities)

  val feedListener = new FeedListener[(Set[DBpediaResource], Text, Map[DBpediaResource,Int])]() {
    protected def update(item: (Set[DBpediaResource], Text, Map[DBpediaResource,Int])) {
      train(item._1, item._3)
    }
  }

  private var updateCtr = 0
  private var meanSquaredError = 0.0

  def train(targets: Set[DBpediaResource], annotations: Map[DBpediaResource,Int]) {
    LOG.info("Updating target-entity classifier...")
    updateCtr += 1

    if (evalWriter!=null) {
      val predictions = classifier.getPredictions(annotations)
      meanSquaredError += predictions.foldLeft(0.0)((sum,prediction) =>
        sum + math.pow( { if(targets.contains(prediction._1)) 1.0 else 0.0 } - prediction._2, 2))

      if (updateCtr%evalInterval==0) {
        evalWriter.println("===== Evaluation of interval "+(updateCtr-evalInterval)+" - "+updateCtr+" =====")
        evalWriter.println("mean squared error: "+(meanSquaredError/evalInterval))
        evalWriter.println("last predictions:"+predictions.foldLeft("")( (string,prediction) => " ("+prediction._2+","+prediction._1.uri+")"))
        evalWriter.println("last should:"+targets.foldLeft("")( (string,target) => " "+target.uri))
        evalWriter.println("last annotations:"+annotations.foldLeft("")( (string,annotation) => " ("+annotation._2+","+annotation._1.uri+")"))
        meanSquaredError = 0.0
      }
    }

    classifier.update(targets, annotations)
  }

  def stopTraining {
    feedListener.unSubscribeToAllFeeds
    if (evalWriter!=null)
      evalWriter.close()
  }

  def saveModel {
    classifier.persist
  }

  def subscribeToAll {
    FeedRegistry.getFeeds[(Set[DBpediaResource], Text, Map[DBpediaResource,Int])].foreach(feed => feed.subscribe(feedListener))
  }
}
