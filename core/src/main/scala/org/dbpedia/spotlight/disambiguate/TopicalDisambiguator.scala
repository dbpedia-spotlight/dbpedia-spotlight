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
import org.dbpedia.spotlight.lucene.disambiguate.MergedOccurrencesDisambiguator
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
import org.dbpedia.spotlight.db.model.TopicalPriorStore
import org.dbpedia.spotlight.topics.TopicExtractor

/**
 * Uses only topic prior to decide on disambiguation.
 * Baseline disambiguator.For evaluation only.
 *
 * @author pablomendes
 */
class TopicalDisambiguator(val candidateSearcher: CandidateSearcher,val topicalPriorStore: TopicalPriorStore)
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
                    case e: ItemNotFoundException => LOG.debug(e);
                }
                //ATTENTION there is no r.support at this point
                //TODO if support comes from candidate index, it means c(sf,r).

                LOG.trace("# candidates for: %s = %s.".format(sfOcc.surfaceForm, candidates.size))
                candidates.foreach(r => allCandidates.add(r))
                acc + (sfOcc -> candidates.toList)
            });
        val e1 = System.nanoTime()
        //LOG.debug("Time with %s: %f.".format(m1, (e1-s1) / 1000000.0 ))
        occs
    }

    def getTopicalScore(textTopics: Map[String,Double], resource: DBpediaResource) : Double = {//TODO Topic->Double
        val resourceTopicCounts = topicalPriorStore.getTopicalPriorCounts(resource)
        val topicTotals = topicalPriorStore.getTotalCounts()
        LOG.trace("resource: %s".format(resource.uri))
        val score = textTopics.map{ case (topic,textScore) => {
            val total = topicTotals.get(topic) match {
                case Some(n) => n
                case None => throw new SearchException("Topic set was not loaded correctly.")
            }
            val resourceCount = resourceTopicCounts.getOrElse(topic, 0).toDouble
            val resourcePrior = resourceCount / total.toDouble
            val logRP = if (resourcePrior==0) 0.0 else math.log(resourcePrior)
            val logTS = if (textScore==0) 0.0 else math.log(textScore)
            if (resourcePrior>0.0)
                LOG.trace("\t\ttopic: %s, resource prior: %.5f".format(topic,resourcePrior))
            logRP + logTS
          }
        }.sum
        math.exp(score)
    }

    def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {

        LOG.debug("Running bestK for paragraph %s.".format(paragraph.id))

        if (paragraph.occurrences.size == 0) return Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()

        val topics = TopicExtractor.getTopics(paragraph.text.text)
        LOG.trace("text: %s".format(topics.filter(_._2>0).toMap.toString))

        //        val m1 = if (candLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"
        //        val m2 = if (contextLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"

        // step1: get candidates for all surface forms
        //       (TODO here building allCandidates directly, but could extract from occs)
        var allCandidates = CompactHashSet[DBpediaResource]();
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
        });

        // LOG.debug("Reranked (%d)".format(r.size))

        r
    }

    def name(): String = {
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
