/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.disambiguate

import scala._
import org.dbpedia.spotlight.model._
import scalaj.collection.Imports._
import org.dbpedia.spotlight.topical.TopicalClassifier
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.db.model.{ResourceTopicsStore, TopicalPriorStore}
import com.officedepot.cdap2.collection.CompactHashSet
import org.dbpedia.spotlight.exceptions.ItemNotFoundException
import org.apache.commons.logging.LogFactory

/**
 *
 * @author pablomendes, dirkweissenborn
 */

class TopicalFilteredDisambiguator(candidateSearcher: CandidateSearcher,
                               contextSearcher: MergedOccurrencesContextSearcher, //TODO should be ContextSearcher. Need a generic disambiguator to enable this.
                               val topicalPriorStore: TopicalPriorStore,
                               val resourceTopicStore:ResourceTopicsStore,
                               val minimalConfidence: Double,
                               val classifier: TopicalClassifier)
    extends TwoStepDisambiguator(candidateSearcher,contextSearcher) {

    private val LOG = LogFactory.getLog(this.getClass)

    if(classifier==null)
        LOG.error("Topical classifier is null!")

    private var topicalScores : Map[DBpediaResource,Double] = null

    //Careful: Copied code from TwoStepDisambiguator
    //If you want us to extract allCandidates from occs while we're generating it, you have to pass in the allCandidates parameter
    //If you don't pass anything down, we will just fill in a dummy hashset and let the garbage collector deal with it
    override def getCandidates(paragraph: Paragraph, allCandidates: CompactHashSet[DBpediaResource] = CompactHashSet[DBpediaResource]()) = {
        val topics = classifier.getPredictions(paragraph.text)

        // step1: get candidates for all surface forms (TODO here building allCandidates directly, but could extract from occs)
        val occs = paragraph.occurrences
            .foldLeft( Map[SurfaceFormOccurrence,List[DBpediaResource]]())(
            (acc,sfOcc) => {
                LOG.debug("searching...")
                var candidates = new java.util.HashSet[DBpediaResource]().asScala
                try {
                    candidates = candidateSearcher.getCandidates(sfOcc.surfaceForm).asScala //.map(r => r.uri)
                } catch {
                    case e: ItemNotFoundException => LOG.debug(e)
                }
                //ATTENTION there is no r.support at this point
                //TODO if support comes from candidate index, it means c(sf,r).

                //LOG.debug("# candidates for: %s = %s (%s)".format(sfOcc.surfaceForm,candidates.size,candidates))
                LOG.debug("# candidates for: %s = %s.".format(sfOcc.surfaceForm,candidates.size))
                candidates.foreach( r => allCandidates.add(r))
                acc + (sfOcc -> candidates.toList)
            })

        var bestTopicalScores = Map[SurfaceFormOccurrence,Double]()

        topicalScores = occs.foldLeft(Map[DBpediaResource,Double]())((tmpTopicalScores,occ) => {
            tmpTopicalScores ++ occ._2.foldLeft(Map[DBpediaResource,Double]())((acc,resource) => {
                val score = tmpTopicalScores.getOrElse(resource, getTopicalScore(topics,resource, (d1,d2) => d1+d2 ) )

                if (score > bestTopicalScores.getOrElse(occ._1,0.0))
                    bestTopicalScores += (occ._1 -> score)

                acc + (resource -> score)
            })

        })

        bestTopicalScores.foreach {
            case (occ,bestScore)=>
                if (bestScore > 0.3) {
                    occs(occ).foreach( resource => if (topicalScores.getOrElse(resource,0.0) <= 0.16) allCandidates.remove(resource) )
                }
        }

        occs
    }

    def getTopicalScore(textTopics: Array[(Topic, Double)], resource: DBpediaResource, f:(Double,Double)=>Double): Double = {
        val resourceTopicCounts = topicalPriorStore.getTopicalPriorCounts(resource)//.filterNot(_._1.equals(TopicUtil.CATCH_TOPIC))
        //val topicTotals = topicalPriorStore.getTotalCounts()//.filterNot(_._1.equals(TopicUtil.CATCH_TOPIC))
        val resourceTotal =  resourceTopicCounts.values.sum.toDouble

        var score = 0.0
        // p( r | c) = Sum ( p(r|t) * p(t|c) )
        // priors p(r|t)
        /*val (resourcePriorsForTopic,resourcePriorsForNotTopic) =
            textTopics.foldLeft((Map[Topic,Double](),Map[Topic,Double]())) {
                case ((priorsT,priorsNT),(topic,_)) => {
                    val topicTotal = topicTotals.get(topic) match {
                        case Some(n) => n
                        case None => throw new SearchException("Topic set was not loaded correctly.")
                    }
                    val resourceCount = resourceTopicCounts.getOrElse(topic, 0).toDouble
                    (priorsT + (topic -> (resourceCount+1) / topicTotal.toDouble) ,
                        priorsNT + (topic-> (resourceTopicCounts.values.sum-resourceCount+textTopics.size+1) / (total-topicTotal).toDouble) )
                }
            }

        // different scores for multi-label and single label
        if (classifier.isInstanceOf[MultiLabelClassifier])
            score = textTopics.foldLeft(1.0){ case (acc,(topic,textScore)) => {
                acc * math.max(textScore * resourcePriorsForTopic(topic), (1-textScore)*resourcePriorsForNotTopic(topic))
            }}
        else
            score = textTopics.foldLeft(0.0){ case (acc,(topic,_)) => {
                var summand = 1.0
                textTopics.foreach{ case(otherTopic,textScore) => {
                    summand *=  { if(otherTopic.equals(topic)) textScore * resourcePriorsForTopic(topic) else (1-textScore)* resourcePriorsForNotTopic(topic) }
                }}
                acc + summand
            }} */

        //calculate priors p(topic|resource)
        var topicPriorsForResource = Map[Topic,Double]() // resourceTopicStore.pTopicGivenResource(resource)
        topicPriorsForResource =
        textTopics.foldLeft(Map[Topic,Double]()) {
            case (priorsT,(topic,_)) => {
                val resourceCount = resourceTopicCounts.getOrElse(topic, 0).toDouble
                priorsT + (topic -> (resourceCount+1) / (resourceTotal+textTopics.size))
            }
        }


        // different scores for multi-label and single label
        //val sum = textTopics.foldLeft(0.0)( _ + _._2 )

        //Dot product if f is chosen to be the '+' operator
        score = textTopics.sortBy(_._1.getName).map(_._2).zip(topicPriorsForResource.toList.sortBy(_._1.getName).map(_._2)).
            foldLeft(0.0)((acc, predictions) => f(acc,predictions._1*predictions._2))


        //DISTANCE BASED
        /*val distance = getDistance(textTopics.sortBy(_._1.getName).map(_._2).toArray, topicPriorsForResource.toList.sortBy(_._1.getName).map(_._2).toArray)
        score = 1.0/(distance+1.0) */


        //OUTPUT
        /*
        LOG.info("Resource: "+resource.uri)
        LOG.info("Priors: "+topicPriorsForResource.toList.sortBy(-_._2).foldLeft("")((acc,prediction)=> acc+" %s:%.3f".format(prediction._1.getName,prediction._2)))
        LOG.info("Predictions: "+textTopics.sortBy(-_._2).foldLeft("")((acc,prediction)=> acc+" %s:%.3f".format(prediction._1.getName,prediction._2)))
        LOG.info("Score: "+score)    */

        score
    }

    override def bestK(paragraph:  Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {
        val bestk = super.bestK(paragraph,k)
       /*  bestk.foreach { case (sfOcc,resources) => {
            resources.foreach( occ => {
                occ.topicalScore = topicalScores(occ.resource)
            })
        } }      */

        bestk
    }

    override def name(): String = {
        "TopicalFilteredDisambiguator"
    }

}
