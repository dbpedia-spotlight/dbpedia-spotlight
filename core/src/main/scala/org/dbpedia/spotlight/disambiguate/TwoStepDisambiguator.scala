/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.disambiguate

import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.lucene.similarity._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.lucene.disambiguate.MergedOccurrencesDisambiguator
import java.lang.UnsupportedOperationException
import scalaj.collection.Imports._
import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import org.apache.lucene.search.similar.MoreLikeThis
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, SearchException, InputException}
import org.apache.lucene.search.{ScoreDoc, FieldCacheTermsFilter, Explanation, TermsFilter}
import collection.mutable.{HashMap, HashSet}
import java.io.{ByteArrayInputStream, File}
import org.dbpedia.spotlight.model._
import org.apache.lucene.index.Term
import org.dbpedia.spotlight.lucene.search.{CandidateSearcher, MergedOccurrencesContextSearcher}
import com.officedepot.cdap2.collection.CompactHashSet

/**
 * Paragraph disambiguator that queries paragraphs once and uses candidate map to filter results.
 * 1) performs candidate selection: searches sf -> uri
 * 2) context score: searches context with filter(uri)
 * 3) disambiguates by ranking candidates found in 1 according to score from 2.
 *
 * @author pablomendes
 */
class TwoStepDisambiguator(val configuration: SpotlightConfiguration) extends ParagraphDisambiguator  {

    private val LOG = LogFactory.getLog(this.getClass)

    LOG.info("Initializing disambiguator object ...")

    val contextIndexDir = LuceneManager.pickDirectory(new File(configuration.getContextIndexDirectory))
    val candidateIndexDir = LuceneManager.pickDirectory(new File(configuration.getCandidateIndexDirectory))

    val contextLuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(contextIndexDir) // use this if all surface forms in the index are lower-cased
    val cache = JCSTermCache.getInstance(contextLuceneManager, configuration.getMaxCacheSize);
    contextLuceneManager.setContextSimilarity(new CachedInvCandFreqSimilarity(cache))        // set most successful Similarity
    contextLuceneManager.setDBpediaResourceFactory(configuration.getDBpediaResourceFactory)
    contextLuceneManager.setDefaultAnalyzer(configuration.getAnalyzer)
    val contextSearcher = new MergedOccurrencesContextSearcher(contextLuceneManager)

    //val candidateSearcher : CandidateSearcher = contextSearcher; // here we can reuse the same object because it implements both CandidateSearcher and ContextSearcher interfaces
    val candLuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(candidateIndexDir) // use this if surface forms in the index are case-sensitive
    candLuceneManager.setDBpediaResourceFactory(configuration.getDBpediaResourceFactory)

    val candidateSearcher = new CandidateSearcher(candLuceneManager) // or we can provide different functionality for surface forms (e.g. n-gram search)

    val disambiguator : Disambiguator = new MergedOccurrencesDisambiguator(contextSearcher)

    LOG.info("Done.")

    @throws(classOf[InputException])
    def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence] = {
        // return first from each candidate set
        bestK(paragraph, 5).map( kv => kv._2(0) ).toList
    }

    //WARNING: this is repetition of BaseSearcher.getHits
    //TODO move to subclass of BaseSearcher
    def query(text: Text, allowedUris: Array[DBpediaResource]) = {
        //val filter = new FieldCacheTermsFilter(DBpediaResourceField.CONTEXT.toString,allowedUris)
        val filter = new org.apache.lucene.search.TermsFilter()
        allowedUris.foreach( u => filter.addTerm(new Term(DBpediaResourceField.URI.toString,u.uri)) )
        //val filter = null;
        val mlt = new MoreLikeThis(contextSearcher.mReader);
        mlt.setFieldNames(Array(DBpediaResourceField.CONTEXT.toString))
        mlt.setAnalyzer(contextLuceneManager.defaultAnalyzer)
        LOG.debug("Analyzer %s".format(contextLuceneManager.defaultAnalyzer))
        val inputStream = new ByteArrayInputStream(text.text.getBytes("UTF-8"));
        val query = mlt.like(inputStream);
        contextSearcher.getHits(query, allowedUris.size, 50000, filter)
    }


    //TODO break down into two steps: candidates and context query
    def bestK(paragraph:  Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]= {

        LOG.debug("Running bestK for paragraph %s.".format(paragraph.id))

        if (paragraph.occurrences.size==0) return Map[SurfaceFormOccurrence,List[DBpediaResourceOccurrence]]()

        val m1 = if (candLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"
        val m2 = if (contextLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"

        val s1 = System.nanoTime()
        // step1: get candidates for all surface forms (TODO here building allCandidates directly, but could extract from occs)
        var allCandidates = CompactHashSet[DBpediaResource]();
        val occs = paragraph.occurrences
            .foldLeft( Map[SurfaceFormOccurrence,List[DBpediaResource]]())(
            (acc,sfOcc) => {
                val candidates = candidateSearcher.getCandidates(sfOcc.surfaceForm).asScala //.map(r => r.uri)
                LOG.debug("# candidates for: %s = %s (%s)".format(sfOcc.surfaceForm,candidates.size,candidates))
                candidates.foreach( r => allCandidates.add(r))
                acc + (sfOcc -> candidates.toList)
            });
        val e1 = System.nanoTime()
        LOG.debug("Time with %s: %f.".format(m1, (e1-s1) / 1000000.0 ))

        val s2 = System.nanoTime()
        // step2: query once for the paragraph context, get scores for each candidate resource
        val hits = query(paragraph.text, allCandidates.toArray)
        LOG.debug("Hits (%d): %s".format(hits.size, hits.map( sd => "%s=%s".format(sd.doc,sd.score) ).mkString(",")))
        val scores = hits
            .foldRight(Map[String,Double]())((hit,acc) => {
            var resource: DBpediaResource = contextSearcher.getDBpediaResource(hit.doc)
            var score = hit.score
            acc + (resource.uri -> score)
        });
        val e2 = System.nanoTime()
        LOG.debug("Scores (%d): %s".format(scores.size, scores))

        LOG.debug("Time with %s: %f.".format(m2, (e2-s2) / 1000000.0 ))

        // pick the best k for each surface form
        occs.keys.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())( (acc,aSfOcc) => {
            val candOccs = occs(aSfOcc)
                .map( resource => Factory.DBpediaResourceOccurrence.from(aSfOcc,
                                                                  resource,
                                                                  scores.getOrElse(resource.uri,0.0)) )
                .sortBy(o => o.contextualScore)
                .reverse
                .take(k)
            acc + (aSfOcc -> candOccs)
        });
    }

    def name() : String = {
        "2step+"+disambiguator.name
    }

    def ambiguity(sf : SurfaceForm) : Int = {
        candidateSearcher.getAmbiguity(sf)
    }

    def support(resource : DBpediaResource) : Int = {
        disambiguator.support(resource)
    }

    @throws(classOf[SearchException])
    def explain(goldStandardOccurrence: DBpediaResourceOccurrence, nExplanations: Int) : List[Explanation] = {
        disambiguator.explain(goldStandardOccurrence, nExplanations).asScala.toList
    }

    def contextTermsNumber(resource : DBpediaResource) : Int = {
        disambiguator.contextTermsNumber(resource)
    }

    def averageIdf(context : Text) : Double = {
        disambiguator.averageIdf(context)
    }


    //TODO better than throw exception, we should split the interface Disambiguator accordingly
    def disambiguate(sfOccurrence: SurfaceFormOccurrence): DBpediaResourceOccurrence = {
        throw new UnsupportedOperationException("Cannot disambiguate single occurrence. This disambiguator uses multiple occurrences in the same paragraph as disambiguation context.")
    }
    def bestK(sfOccurrence: SurfaceFormOccurrence, k: Int): java.util.List[DBpediaResourceOccurrence] = {
        throw new UnsupportedOperationException("Cannot disambiguate single occurrence. This disambiguator uses multiple occurrences in the same paragraph as disambiguation context.")
    }

}