package org.dbpedia.spotlight.model.trec

import org.dbpedia.spotlight.model.{DecoratorFeed, Feed, FeedListener}
import org.dbpedia.spotlight.model.{Topic, Text, DBpediaResource}
import org.dbpedia.spotlight.topic.utility.TopicUtil
import org.dbpedia.spotlight.db.model.TopicalPriorStore
import collection.mutable._
import collection.mutable
import org.apache.commons.logging.LogFactory

/**
 * This is a decorator for the TrecResourceAnnotationFeed (member textAnnotationFeed) class, which assigns, based on a set of resource annotations
 * and a specified minimal confidence, topic labels to a streamed in text.
 * @param topicalPriors Loaded topical priors (see: TopicalPriorStore)
 * @param feed Should be a TrecResourceAnnotationFeed
 */
class TrecTopicTextFromAnnotationsFeed(topicalPriors: TopicalPriorStore, feed: Feed[(Set[DBpediaResource],Text, Map[DBpediaResource,Double])])
  extends DecoratorFeed[(Set[DBpediaResource],Text, Map[DBpediaResource,Double]),(Map[Topic,Double],Text)](feed, true) {

  private val LOG = LogFactory.getLog(getClass)

  def processFeedItem(item: (Set[DBpediaResource], Text, Map[DBpediaResource,Double])) {
    LOG.debug("Annotating DBpediaResources+Text with Topic...")

    val (targets, text, annotations) = item
    LOG.debug("Resources:"+annotations.foldLeft("")((string,annotation) => string+" "+annotation._1.uri))

    var probabilitiesAnnotations = Map[Topic,Double]()
    var normalizingConstant = 0.0

    //p(t|r1,r2,...) = p(r1,r2,...|t) * p(t) / p(r1,r2,...)
    //p(r1,r2,...|t) = c(r1,r2,...,t)/c(t)                      Assumption: c(r1,r2,...,t)= Sum(c(ri,t))/n , n-nr of resources
    //p(t) = c(t)/c                                             c- overall count
    //p(r1,r2,...) = c(r1,r2,...) / c                           Assumption: c(r1,r2,...)= Sum(c(ri))/n
    //  -> p(t|r1,r2,...) = c(r1,r2,...,t) / c(r1,r2,...) = Sum(c(ri,t)) / Sum(c(ri))

    annotations.foreach { case (resource, occCount) => {
      val counts = topicalPriors.getTopicalPriorCounts(resource).filter(!_._1.equals(TopicUtil.CATCH_TOPIC))
      val resourceSum =  counts.values.sum.toDouble

      if (resourceSum > 0.0) {
        val weight = resourceSum/(resourceSum+topicalPriors.getTopicalPriorCount(resource,TopicUtil.CATCH_TOPIC))
        normalizingConstant += resourceSum * occCount * weight

        counts.filter(_._2 > 0).foreach {
          case (topic, count) => {
            if(probabilitiesAnnotations.contains(topic))
              probabilitiesAnnotations(topic) += count.toDouble * occCount * weight
            else
              probabilitiesAnnotations += (topic -> count.toDouble * occCount * weight)
          }
        }
      }
    }}
    probabilitiesAnnotations = Map() ++ probabilitiesAnnotations.mapValues(_/normalizingConstant)

    normalizingConstant = 0.0
    var probabilitiesTargets = Map[Topic,Double]()
    targets.foreach(resource => {
      val counts = topicalPriors.getTopicalPriorCounts(resource).filter(!_._1.equals(TopicUtil.CATCH_TOPIC))
      val resourceSum =  counts.values.sum.toDouble

      if (resourceSum > 0.0) {
        val weight = resourceSum/(resourceSum+topicalPriors.getTopicalPriorCount(resource,TopicUtil.CATCH_TOPIC))
        normalizingConstant += resourceSum * weight

        counts.filter(_._2 > 0).foreach {
          case (topic, count) => {
            if(probabilitiesTargets.contains(topic))
              probabilitiesTargets(topic) += count.toDouble * weight
            else
              probabilitiesTargets += (topic -> count.toDouble * weight)
          }
        }
      }
    })
    probabilitiesTargets = Map() ++ probabilitiesTargets.mapValues(_/normalizingConstant)

    val keys = probabilitiesAnnotations.keySet ++ probabilitiesTargets.keySet
    var probabilities = Map[Topic,Double]()

    keys.foreach( key => probabilities += ( key -> (probabilitiesAnnotations.getOrElse(key,0.0)/2.0 + probabilitiesTargets.getOrElse(key,0.0)/2.0) ) )
    probabilitiesAnnotations.foreach{ case (topic, probability) =>
      LOG.debug("Assigned topic: "+topic.getName+" -> "+probability)
    }
    if(probabilities.size > 0)
      notifyListeners((probabilities, text))  }
}
