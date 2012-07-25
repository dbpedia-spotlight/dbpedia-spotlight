package org.dbpedia.spotlight.model.trec

import org.dbpedia.spotlight.model.{DecoratorFeed, Feed, FeedListener}
import org.dbpedia.spotlight.model.{Topic, Text, DBpediaResource}
import org.dbpedia.spotlight.topic.utility.TopicUtil
import org.dbpedia.spotlight.db.model.TopicalPriorStore
import collection.mutable._
import collection.mutable
import org.apache.commons.logging.LogFactory

/**
 * This is a decorator of the TrecResourceAnnotationFeed class, which assigns, based on a set of resource annotations,
 * topic labels to a streamed in text.
 * @param topicalPriors Loaded topical priors (see: TopicalPriorStore)
 * @param minimalConfidence minimal confidence of assigning a topical label to a given input (depends on the topical priors)
 * @param feed Should be a TrecResourceAnnotationFeed
 */
class TrecTopicTextFromAnnotationsFeed(topicalPriors: TopicalPriorStore, minimalConfidence: Double, feed: Feed[(Set[DBpediaResource], Text, Map[DBpediaResource,Int])])
  extends DecoratorFeed[(Set[DBpediaResource], Text, Map[DBpediaResource,Int]),(Set[Topic],Text)](feed, true) {

  private val LOG = LogFactory.getLog(getClass)

  def processFeedItem(item: (Set[DBpediaResource], Text, Map[DBpediaResource,Int])) {
    LOG.debug("Annotating DBpediaResources+Text with Topic...")

    val (targets, text, annotations) = item
    LOG.debug("Resources:"+annotations.foldLeft("")((string,annotation) => string+" "+annotation._1.uri))

    val topicCounts = topicalPriors.getTotalCounts()
    val totalSum = topicCounts.values.sum.toDouble
    var probabilities = Map() ++ topicalPriors.getTotalCounts().transform( (topic, count) => count.toDouble/totalSum )
    var normalizingConstant = 1.0

    //TODO check!
    //p(t|r1,r2,...) = p(r1,r2,...|t) * p(t) / p(r1,r2,...)
    //p(r1,r2,...|t) = p(r1|t)*p(r2|t)*...    (Assumption)
    //p(r1,r2,...)=p(r1)*p(r2)*...            (Assumption)

    annotations.foreach { case (resource, occCount) => {
      val counts = topicalPriors.getTopicalPriorCounts(resource).filter(!_._1.equals(TopicUtil.CATCH_TOPIC))
      val resourceSum =  counts.values.sum.toDouble

      if (resourceSum > 0.0) {
        normalizingConstant *= math.pow(resourceSum/totalSum,occCount)

        counts.filter(_._2 > 0).foreach {
          case (topic, count) => {
            if (probabilities.contains(topic))
              probabilities(topic) *= math.pow(count.toDouble/topicCounts(topic), occCount)
            else
              probabilities += (topic -> math.pow(count.toDouble/topicCounts(topic), occCount))
          }
        }
      }
    }}

    probabilities = Map() ++ probabilities.mapValues(_/normalizingConstant)

    var topics = Set[Topic]()
    probabilities.foreach{ case (topic, probability) =>
      //TODO check!
      if(probability > minimalConfidence) {
        LOG.debug("Assigned topic: "+topic.getName+" -> "+probability)
        topics += (topic)
      }
    }
    notifyListeners((topics, text))
  }
}
