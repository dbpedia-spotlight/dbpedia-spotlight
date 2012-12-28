package org.dbpedia.spotlight.feed

import org.dbpedia.spotlight.model._
import scala.Double
import org.dbpedia.spotlight.db.model.TopicalPriorStore
import org.dbpedia.spotlight.topical.util.TopicInferrer
import org.apache.commons.logging.LogFactory
import collection.mutable._

/**
 * This feed actually transforms feed items from the wikipedia update stream, by inferring the topic of the text given the
 * wikipedia annotations within the text and the resource of the article this text appeared in.
 * @param topicalPriors
 * @param wikipediaFeed
 */
class WikiTopicInferenceFeed(topicalPriors: TopicalPriorStore, wikipediaFeed: Feed[(DBpediaResource, Set[DBpediaResourceOccurrence], Set[DBpediaCategory], Text)])
    extends DecoratorFeed[(DBpediaResource, Set[DBpediaResourceOccurrence], Set[DBpediaCategory], Text), (Map[Topic, Double], Text)](wikipediaFeed, true) {

    private val topicInferrer = new TopicInferrer(topicalPriors)
    private val LOG = LogFactory.getLog(getClass)

    def processFeedItem(item: (DBpediaResource, Set[DBpediaResourceOccurrence], Set[DBpediaCategory], Text)) {
        LOG.debug("Annotating DBpediaResources+Text with Topic...")

        val (target, annotations, categories, text) = item
        LOG.debug("Main resource: " + target.uri)
        LOG.debug("Resources:" + annotations.foldLeft("")((string, annotation) => string + " " + annotation.resource.uri))

        val probabilities = topicInferrer.inferTopics(
            annotations.foldLeft(Map[DBpediaResource, Double]())((map, occ) => {
                if (map.contains(occ.resource))
                    map
                else
                    map + (occ.resource -> annotations.filter(_.resource.equals(occ.resource)).size.toDouble)
            }), Set(target))

        probabilities.foreach {
            case (topic, probability) =>
                LOG.debug("Assigned topic: " + topic.getName + " -> " + probability)
        }
        if (probabilities.size > 0)
            notifyListeners((probabilities, text))
    }


}
