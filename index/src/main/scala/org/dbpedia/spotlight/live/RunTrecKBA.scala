package org.dbpedia.spotlight.live

import model.trec.{TrecTopicTextFromAnnotationsFeed, TrecResourceAnnotationFeed, TrecCorpusFeed}
import org.dbpedia.spotlight.model.{DBpediaResource, SpotlightFactory, SpotlightConfiguration}
import org.apache.commons.lang.time.DateUtils
import java.io.{BufferedReader, InputStreamReader, FileNotFoundException, File}
import org.dbpedia.spotlight.db.model.HashMapTopicalPriorStore
import trainer.{TrecLiveTargetEntityTrainer, TopicLiveTrainer}
import io.Source
import org.apache.commons.logging.LogFactory
import scala.collection.mutable._
import org.dbpedia.spotlight.topic.utility.TextVectorizer
import actors.Actor

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 7/18/12
 * Time: 11:57 AM
 * To change this template use File | Settings | File Templates.
 */

object RunTrecKBA {

  private val LOG = LogFactory.getLog(getClass)

  /**
   *
   * @param args spotlight configuration, trec corpus dir, trecJudgmentsFile, training start date (yyyy-MM-dd-hh), end date,
   *             minimal confidence of assigning topic to a list of resources, path to trec target entity classifier model dir,
   *             evaluation folder (evaluation, if folder exists, no evaluation otherwise), clear (optional, start training from scratch except of topical classifier)
   */
  def main(args:Array[String]) {
    var configuration: SpotlightConfiguration = null
    //Initialization, check values
    try {
      val configFileName: String = args(0)
      configuration = new SpotlightConfiguration(configFileName)
    }
    catch {
      case e: Exception => {
        e.printStackTrace
        System.exit(1)
      }
    }
    val corpusDir = new File(args(1))
    if (!corpusDir.exists())
      throw new FileNotFoundException("Corpus not found in directory "+args(1))

    val trecJudgmentsFile = new File(args(2))
    if (!trecJudgmentsFile.exists())
      throw new FileNotFoundException("Judgment file not found at "+args(2))

    val startDate = DateUtils.parseDate(args(3),Array("yyyy-MM-dd-hh"))
    val endDate = DateUtils.parseDate(args(4),Array("yyyy-MM-dd-hh"))
    val minimalConfidence = args(5).toDouble

    val targetClassifierModelDir = new File(args(6))

    var evalFolder = new File(args(7))
    if (!evalFolder.exists())
      evalFolder = null

    val clear = args.length > 8 && args(8).equals("clear")

    if(clear) {
      targetClassifierModelDir.listFiles().foreach(file => {
        if (file.isDirectory)
          file.listFiles().foreach(_.delete())
        else
          file.delete()
      })
      evalFolder.listFiles().foreach(_.delete())
    }

    val factory = new SpotlightFactory(configuration)

    //Feed setup
    val corpusFeed = new TrecCorpusFeed(corpusDir, startDate, endDate, trecJudgmentsFile, clear)
    val annotationFeed = new TrecResourceAnnotationFeed(factory.annotator(), corpusFeed)
    annotationFeed.start
    val trecTopicTextFeed = new TrecTopicTextFromAnnotationsFeed(HashMapTopicalPriorStore, minimalConfidence, annotationFeed)
    trecTopicTextFeed.start

    //Trainer
    val topicalClassifierTrainer = new TopicLiveTrainer(factory.topicalClassifier,new File(evalFolder,"topic_training.eval"))
    val trecTargetEntityTrainer = new TrecLiveTargetEntityTrainer(targetClassifierModelDir, corpusFeed.targetEntities, new File(evalFolder,"target_entity_training.eval"))

    LOG.info("Starting training!")
    corpusFeed.start()

    var curLine = ""
    LOG.info("Type 'quit' to quit training")

    val converter = new InputStreamReader(System.in)
    val in = new BufferedReader(converter)

    while (!((curLine == "quit"))) {
      curLine = in.readLine
    }
    LOG.info("Stopping training! Wait until it is finishes...")
    corpusFeed.stopFeed

    while(!corpusFeed.getState.equals(Actor.State.Terminated))
      Thread.sleep(1000)

    trecTargetEntityTrainer.stopTraining
    topicalClassifierTrainer.stopTraining
    trecTargetEntityTrainer.saveModel
    topicalClassifierTrainer.saveModel
  }
}
