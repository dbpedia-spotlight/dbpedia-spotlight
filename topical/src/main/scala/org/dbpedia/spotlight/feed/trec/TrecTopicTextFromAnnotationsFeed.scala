package org.dbpedia.spotlight.feed.trec

import org.dbpedia.spotlight.model.Topic
import org.dbpedia.spotlight.feed.util.{TopicInferrer, TopicUtil}
import scala.Double

/**
 * This is a decorator for the TrecResourceAnnotationFeed (member textAnnotationFeed) class, which assigns, based on a set of resource annotations
 * and a specified minimal confidence, topic labels to a streamed in text.
 * @param topicalPriors Loaded topical priors (see: TopicalPriorStore)
 * @param feed Should be a TrecResourceAnnotationFeed
 */
class TrecTopicTextFromAnnotationsFeed(topicalPriors: TopicalPriorStore, feed: Feed[(Set[DBpediaResource], Text, Map[DBpediaResource, Double])])
    extends DecoratorFeed[(Set[DBpediaResource], Text, Map[DBpediaResource, Double]), (Map[Topic, Double], Text)](feed, true) {

    private val topicInferrer = new TopicInferrer(topicalPriors)
    private val LOG = LogFactory.getLog(getClass)

    def processFeedItem(item: (Set[DBpediaResource], Text, Map[DBpediaResource, Double])) {
        LOG.debug("Annotating DBpediaResources+Text with Topic...")

        val (targets, text, annotations) = item
        LOG.debug("Resources:" + annotations.foldLeft("")((string, annotation) => string + " " + annotation._1.uri))

        val probabilities = topicInferrer.inferTopics(annotations, targets)
        probabilities.foreach {
                LOG.debug("Assigned topic: " + topic.getName + " -> " + probability)
        }
        if (probabilities.size > 0)
            notifyListeners((probabilities, text))
    }


}
