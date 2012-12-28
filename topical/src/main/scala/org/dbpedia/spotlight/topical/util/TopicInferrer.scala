package org.dbpedia.spotlight.topical.util

import org.dbpedia.spotlight.model.{Topic, DBpediaResource}
import scala.collection.mutable._
import org.dbpedia.spotlight.db.model.TopicalPriorStore

/**
 * This class is able to infer topics based on number of occurred resources within a context and optionally also the main resource(s)
 * if given of that context.
 * <br><br>
 * Probabilities of topics based on occurred resources and (if given) the main resource(s) are mixed with equal weights.
 *
 * @param topicalPriors
 */
class TopicInferrer(topicalPriors: TopicalPriorStore) {

    private val topics = topicalPriors.getTotalCounts().keySet.filter(!_.equals(TopicUtil.CATCH_TOPIC))

    def inferTopics(annotations: Map[DBpediaResource, Double], mainResources: Set[DBpediaResource]): Map[Topic, Double] = {
        val probabilitiesAnnotations = inferTopics(annotations)
        val probabilitiesTargets = inferTopics(mainResources.foldLeft(Map[DBpediaResource, Double]())((map, resource) => map + (resource -> 1.0)))

        val keys = probabilitiesAnnotations.keySet ++ probabilitiesTargets.keySet
        var probabilities = Map[Topic, Double]()

        keys.foreach(key => probabilities += (key -> (probabilitiesAnnotations.getOrElse(key, 0.0) / 2.0 + probabilitiesTargets.getOrElse(key, 0.0) / 2.0)))
        probabilities
    }

    def inferTopics(annotations: Map[DBpediaResource, Double]): Map[Topic, Double] = {
        var probabilities = topics.foldLeft(Map[Topic, Double]())((map, topic) => map + (topic -> 0.0))
        var normalizingConstant = 0.0

        //p(t|r1,r2,...) = p(r1,r2,...|t) * p(t) / p(r1,r2,...)
        //p(r1,r2,...|t) = c(r1,r2,...,t)/c(t)                      Assumption: c(r1,r2,...,t)= Sum(c(ri,t))/n , n-nr of resources
        //p(t) = c(t)/c                                             c- overall count
        //p(r1,r2,...) = c(r1,r2,...) / c                           Assumption: c(r1,r2,...)= Sum(c(ri))/n
        //  -> p(t|r1,r2,...) = c(r1,r2,...,t) / c(r1,r2,...) = Sum(c(ri,t)) / Sum(c(ri))

        annotations.foreach {
            case (resource, occCount) => {
                val counts = topicalPriors.getTopicalPriorCounts(resource).filter(!_._1.equals(TopicUtil.CATCH_TOPIC))
                val resourceSum = counts.values.sum.toDouble

                if (resourceSum > 0.0) {
                    val weight = resourceSum / (resourceSum + topicalPriors.getTopicalPriorCount(resource, TopicUtil.CATCH_TOPIC))
                    normalizingConstant += resourceSum * occCount * weight

                    counts.filter(_._2 > 0).foreach {
                        case (topic, count) => {
                            probabilities(topic) += count.toDouble * occCount * weight
                        }
                    }
                }
            }
        }
        if (normalizingConstant > 0.0)
            probabilities = Map() ++ probabilities.mapValues(_ / normalizingConstant)

        probabilities
    }

}
