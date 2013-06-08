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

import org.dbpedia.spotlight.model._
import scala._
import scalaj.collection.Imports._
import org.dbpedia.spotlight.exceptions.{SearchException, ItemNotFoundException, InputException}
import com.officedepot.cdap2.collection.CompactHashSet
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.db.model.TopicalPriorStore
import org.dbpedia.spotlight.topical.{MultiLabelClassifier, TopicalClassifier}
import org.apache.lucene.search.Explanation

/**
 * Uses only topic prior to decide on disambiguation.
 * Baseline disambiguator.For evaluation only.
 * 
 * r ... resource
 * c ... context (text)
 * s ... surfaceform
 * l_i ... one topic labeling (in total there are pow(2,N) labelings possible for multilabel and N for single label)
 * t_k ... topic "k"
 * _t_k ... not topic "k"
 *
 * So we want argmax r p(r | c,s):
 *
 * p(r|c,s) = p(s|c,r) * p(r|c) / p(s|c)
 *
 * p(s|c,r) ...  ? it cannot be one, otherwise s would be subsumed by c), one simple assumption could be that p(s|c)=p(s)
 * p(s|c) ... ?
 *
 * for multilabel:
 *
 * p(r|c) = Sum(p(r,l_i | c)
 *        = Sum( p(l_i|c) * p(r|l_i,c) ) , assuming p(r|l_i,c) = p(r|l_i),
 *
 * sum with pow(2,N) summands !!!! But the sum is governed by the most probable labeling (i.e. max_l ( p(l|c) * p(r|l,c) )) which will be used as heuristic. Note: this is a very simple heurstic and may not
 * reflect reality well.

 * for this sum.
 *
 * l_i(k) = t_k or _t_k
 *
 * p(l_i|c) = Product_k( p(l_i(k) | c) )
 * p(r|l_i) = Product_k( p(r | l_i(k)) )
 *
 * for single label (not implemented yet):
 * Sum( p(l_i|c) * p(r|l_i,c) )  would be linear (with the number of topics) in time because just one label is assigned and this would be computable
 *
 * @author pablomendes, dirk weissenborn
 */
class TopicalDisambiguator(val candidateSearcher: CandidateSearcher,val topicalPriorStore: TopicalPriorStore, val classifier: TopicalClassifier)
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

    //If you want us to extract allCandidates from occs while we're generating it, you have to pass in the allCandidates parameter
    //If you don't pass anything down, we will just fill in a dummy hashset and let the garbage collector deal with it
    def getCandidates(paragraph: Paragraph, allCandidates: CompactHashSet[DBpediaResource] = CompactHashSet[DBpediaResource]()) = {
        val s1 = System.nanoTime()
        // step1: get candidates for all surface forms (TODO here building allCandidates directly, but could extract from occs)
        val occs = paragraph.occurrences
            .foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResource]]())(
            (acc, sfOcc) => {
                LOG.debug("searching...")
                var candidates = new java.util.HashSet[DBpediaResource]().asScala
                try {
                    candidates = candidateSearcher.getCandidates(sfOcc.surfaceForm).asScala //.map(r => r.uri)
                } catch {
                    case e: ItemNotFoundException => LOG.debug(e)
                }
                //ATTENTION there is no r.support at this point
                //TODO if support comes from candidate index, it means c(sf,r).

                LOG.trace("# candidates for: %s = %s.".format(sfOcc.surfaceForm, candidates.size))
                candidates.foreach(r => allCandidates.add(r))
                acc + (sfOcc -> candidates.toList)
            })
        val e1 = System.nanoTime()
        //LOG.debug("Time with %s: %f.".format(m1, (e1-s1) / 1000000.0 ))
        occs
    }

    private def getKLDivergence(distr1:Array[Double], distr2:Array[Double]):Double = {
        var div = 0.0
        val log2 = math.log(2.0)
        for (i <- 0 until distr1.length)
            div += distr1(i)*math.log(distr1(i)/distr2(i))/ log2

        div
    }

    def getTopicalScore(textTopics: Array[(Topic,Double)], resource: DBpediaResource) : Double = {//TODO Topic->Double
        val resourceTopicCounts = topicalPriorStore.getTopicalPriorCounts(resource)
        val topicTotals = topicalPriorStore.getTotalCounts()
        val total = topicTotals.values.sum

        var score = 0.0

        //calculate priors
        val (resourcePriorsForTopic,resourcePriorsForNotTopic) =
            textTopics.foldLeft((Map[Topic,Double](),Map[Topic,Double]())) {
                case ((priorsT,priorsNT),(topic,_)) => {
                    val topicTotal = topicTotals.get(topic) match {
                        case Some(n) => n
                        case None => throw new SearchException("Topic set was not loaded correctly.")
                    }
                    val resourceCount = resourceTopicCounts.getOrElse(topic, 0).toDouble
                    (priorsT + (topic -> resourceCount / topicTotal.toDouble) ,
                     priorsNT + (topic-> (resourceTopicCounts.values.sum-resourceCount) / (total-topicTotal).toDouble) )
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
            }}

        score
    }

    def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {

        LOG.debug("Running bestK for paragraph %s.".format(paragraph.id))

        if (paragraph.occurrences.size == 0) return Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()

        val topics = classifier.getPredictions(paragraph.text)
        LOG.trace("text: %s".format(topics.filter(_._2>0).toString))

        //        val m1 = if (candLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"
        //        val m2 = if (contextLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"

        // step1: get candidates for all surface forms
        //       (TODO here building allCandidates directly, but could extract from occs)
        var allCandidates = CompactHashSet[DBpediaResource]()
        val occs = getCandidates(paragraph, allCandidates)

       // pick the best k for each surface form
        val r = occs.keys.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())((acc, aSfOcc) => {
            val candOccs = occs.getOrElse(aSfOcc, List[DBpediaResource]())
                .map(shallowResource => {

                val supportConfidence = (1, getTopicalScore(topics,shallowResource))

                Factory.DBpediaResourceOccurrence.from(aSfOcc,
                    shallowResource,
                    supportConfidence)
            })
                .sortBy(o => o.contextualScore) //TODO should be final score
                .reverse
                .take(k)
            acc + (aSfOcc -> candOccs)
        })

        // LOG.debug("Reranked (%d)".format(r.size))

        r
    }

    def name = {
        "TopicalDisambiguator"
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
