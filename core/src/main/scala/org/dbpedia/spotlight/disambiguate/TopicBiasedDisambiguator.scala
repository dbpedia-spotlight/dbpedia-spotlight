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

import org.apache.commons.logging.LogFactory
import java.lang.UnsupportedOperationException
import scalaj.collection.Imports._
import org.apache.lucene.search.similar.MoreLikeThis
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, SearchException, InputException}
import org.apache.lucene.search.{ScoreDoc, Explanation}
import org.dbpedia.spotlight.model._
import org.apache.lucene.index.Term
import com.officedepot.cdap2.collection.CompactHashSet
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import java.io.StringReader
import org.dbpedia.spotlight.db.model.{ResourceTopicsStore, TopicalPriorStore}
import org.dbpedia.spotlight.topic.util.{TopicUtil, TopicExtractor}
import org.dbpedia.spotlight.topic.{MultiLabelClassifier, TopicalClassifier}

/**
 *
 * @author pablomendes, dirk weissenborn
 */

class TopicBiasedDisambiguator(val candidateSearcher: CandidateSearcher,
                               val contextSearcher: MergedOccurrencesContextSearcher, //TODO should be ContextSearcher. Need a generic disambiguator to enable this.
                               val topicalPriorStore: TopicalPriorStore,
                               val resourceTopicStore:ResourceTopicsStore,
                               val minimalConfidence: Double,
                               val classifier: TopicalClassifier)
    extends ParagraphDisambiguator {

    private val LOG = LogFactory.getLog(this.getClass)

    @throws(classOf[InputException])
    def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence] = {
        // return first from each candidate set
        bestK(paragraph, 5)
            .filter(kv =>
            kv._2.nonEmpty)
            .map(kv =>
            kv._2.head)
            .toList
    }

    //WARNING: this is repetition of BaseSearcher.getHits
    //TODO move to subclass of BaseSearcher
    def query(text: Text, allowedUris: Array[DBpediaResource]) = {
        LOG.debug("Setting up query.")

        val context = if (text.text.size < 250) text.text.concat(" " + text.text) else text.text //HACK for text that is too short
        //LOG.debug(context)
        val nHits = allowedUris.size
        val filter = new org.apache.lucene.search.TermsFilter() //TODO can use caching? val filter = new FieldCacheTermsFilter(DBpediaResourceField.CONTEXT.toString,allowedUris)
        allowedUris.foreach(u => filter.addTerm(new Term(DBpediaResourceField.URI.toString, u.uri)))

        val mlt = new MoreLikeThis(contextSearcher.mReader);
        mlt.setFieldNames(Array(DBpediaResourceField.CONTEXT.toString))
        mlt.setAnalyzer(contextSearcher.getLuceneManager.defaultAnalyzer)
        //LOG.debug("Analyzer %s".format(contextLuceneManager.defaultAnalyzer))
        //val inputStream = new ByteArrayInputStream(context.getBytes("UTF-8"));
        val query = mlt.like(new StringReader(context), DBpediaResourceField.CONTEXT.toString);
        LOG.debug("Running query.")
        contextSearcher.getHits(query, nHits, 50000, filter)
    }

    //If you want us to extract allCandidates from occs while we're generating it, you have to pass in the allCandidates parameter
    //If you don't pass anything down, we will just fill in a dummy hashset and let the garbage collector deal with it
    def getCandidates(paragraph: Paragraph, allCandidates: CompactHashSet[DBpediaResource] = CompactHashSet[DBpediaResource]()) = {
        val s1 = System.nanoTime()

        //val topics = classifier.getPredictions(paragraph.text)

        // step1: get candidates for all surface forms (TODO here building allCandidates directly, but could extract from occs)
        val occs = paragraph.occurrences
            .foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResource]]())(
            (acc, sfOcc) => {
                LOG.debug("searching...")
                var candidates = new java.util.HashSet[DBpediaResource]().asScala
                try {
                    candidates = candidateSearcher.getCandidates(sfOcc.surfaceForm).asScala/*.filter( res => {
                                                                                                        val score = getTopicalScore(topics, res)
                                                                                                        score > 0.0
                                                                                                    } ) //.map(r => r.uri)  */
                } catch {
                    case e: ItemNotFoundException => LOG.debug(e);
                }
                //ATTENTION there is no r.support at this point
                //TODO if support comes from candidate index, it means c(sf,r).

                //LOG.debug("# candidates for: %s = %s (%s)".format(sfOcc.surfaceForm,candidates.size,candidates))
                LOG.debug("# candidates for: %s = %s.".format(sfOcc.surfaceForm, candidates.size))
                candidates.foreach(r => allCandidates.add(r))
                acc + (sfOcc -> candidates.toList)
            });
        val e1 = System.nanoTime()
        //LOG.debug("Time with %s: %f.".format(m1, (e1-s1) / 1000000.0 ))
        occs
    }

    private def getDistance(distr1:Array[Double], distr2:Array[Double]):Double = {
        var dis = 0.0

        for (i <- 0 until distr1.length)
          dis += math.pow(distr1(i) - distr2(i),2)

        dis
    }

    def getTopicalScore(textTopics: Array[(Topic, Double)], resource: DBpediaResource): Double = {
        val resourceTopicCounts = topicalPriorStore.getTopicalPriorCounts(resource)//.filterNot(_._1.equals(TopicUtil.CATCH_TOPIC))
        val topicTotals = topicalPriorStore.getTotalCounts()//.filterNot(_._1.equals(TopicUtil.CATCH_TOPIC))
        val total = topicTotals.values.sum
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

        //DOT PRODUCT
        score = textTopics.sortBy(_._1.getName).map(_._2).zip(topicPriorsForResource.toList.sortBy(_._1.getName).map(_._2)).
            foldLeft(0.0)((acc, predictions) => acc + predictions._1*predictions._2)


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

    def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {

        LOG.debug("Running bestK for paragraph %s.".format(paragraph.id))

        if (paragraph.occurrences.size == 0) return Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()


        //        val m1 = if (candLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"
        //        val m2 = if (contextLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"

        // step1: get candidates for all surface forms
        //       (TODO here building allCandidates directly, but could extract from occs)
        val allCandidates = CompactHashSet[DBpediaResource]();
        val occs = getCandidates(paragraph, allCandidates)

        val s2 = System.nanoTime()
        // step2: query once for the paragraph context, get scores for each candidate resource

        var hits: Array[ScoreDoc] = null
        try {
            hits = query(paragraph.text, allCandidates.toArray)
        } catch {
            case e: Exception => throw new SearchException(e);
            case r: RuntimeException => throw new SearchException(r);
            case _ => LOG.error("Unknown really scary error happened. You can cry now.")
        }
        // LOG.debug("Hits (%d): %s".format(hits.size, hits.map( sd => "%s=%s".format(sd.doc,sd.score) ).mkString(",")))

        // LOG.debug("Reading DBpediaResources.")
        val topics = classifier.getPredictions(paragraph.text)

        val scores = hits
            .foldRight(Map[String, Tuple3[DBpediaResource, Double,Double]]())((hit, acc) => {
            val resource: DBpediaResource = contextSearcher.getDBpediaResource(hit.doc) //this method returns resource.support=c(r)
            val score = hit.score
            //TODO can mix here the scores: c(s,r) / c(r)
            acc + (resource.uri ->(resource, score, getTopicalScore(topics,resource)))
        })


        val e2 = System.nanoTime()
        //LOG.debug("Scores (%d): %s".format(scores.size, scores))

        //LOG.debug("Time with %s: %f.".format(m2, (e2-s2) / 1000000.0 ))

        // pick the best k for each surface form
        val r = occs.keys.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())((acc, aSfOcc) => {
            val candOccs = occs.getOrElse(aSfOcc, List[DBpediaResource]())
                .map(shallowResource => {
                val (resource: DBpediaResource, supportConfidence: (Int, Double, Double)) = scores.get(shallowResource.uri) match {
                    case Some((fullResource, contextualScore,topicalScore)) => {
                        (fullResource, (fullResource.support, contextualScore,topicalScore))
                    }
                    case _ => (shallowResource, (shallowResource.support, 0.0,0.0))
                }
                /*Factory.DBpediaResourceOccurrence.from(aSfOcc,
                    resource, //TODO this resource may contain the c(s,r) that can be used for conditional prob.
                    supportConfidence)*/
                new DBpediaResourceOccurrence("",  // there is no way to know this here
                    resource, // support is also available from score._1, but types are only in fullResource
                    aSfOcc.surfaceForm,
                    aSfOcc.context,
                    aSfOcc.textOffset,
                    Provenance.Annotation,
                    supportConfidence._2,
                    -1,         // there is no way to know percentage of second here
                    supportConfidence._2,
                    supportConfidence._3)//topical score
            })
                .sortBy(o => 2.9853 * o.contextualScore+ 0.7345*o.topicalScore) //TODO should be final score
                .reverse
                .take(k)
            acc + (aSfOcc -> candOccs)
        });

        // LOG.debug("Reranked (%d)".format(r.size))

        r
    }

    def name(): String = {
        "TopicBiasedDisambiguator"
    }

    def ambiguity(sf: SurfaceForm): Int = {
        candidateSearcher.getAmbiguity(sf)
    }

    def support(resource: DBpediaResource): Int = {
        throw new UnsupportedOperationException("Not implemented.")
    }

    @throws(classOf[SearchException])
    def explain(goldStandardOccurrence: DBpediaResourceOccurrence, nExplanations: Int): List[Explanation] = {
        throw new UnsupportedOperationException("Not implemented.")
    }

    def contextTermsNumber(resource: DBpediaResource): Int = {
        throw new UnsupportedOperationException("Not implemented.")
    }

    def averageIdf(context: Text): Double = {
        throw new UnsupportedOperationException("Not implemented.")
    }


    //TODO better than throw exception, we should split the interface Disambiguator accordingly
    def disambiguate(sfOccurrence: SurfaceFormOccurrence): DBpediaResourceOccurrence = {
        throw new UnsupportedOperationException("Cannot disambiguate single occurrence. This disambiguator uses multiple occurrences in the same paragraph as disambiguation context.")
    }

    def bestK(sfOccurrence: SurfaceFormOccurrence, k: Int): java.util.List[DBpediaResourceOccurrence] = {
        throw new UnsupportedOperationException("Cannot disambiguate single occurrence. This disambiguator uses multiple occurrences in the same paragraph as disambiguation context.")
    }

}
