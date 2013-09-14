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

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.lucene.disambiguate.MergedOccurrencesDisambiguator
import java.lang.UnsupportedOperationException
import scala.collection.JavaConverters._
import org.apache.lucene.search.similar.MoreLikeThis
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, SearchException, InputException}
import org.apache.lucene.search.{ScoreDoc, Explanation}
import org.dbpedia.spotlight.model._
import org.apache.lucene.index.Term
import com.officedepot.cdap2.collection.CompactHashSet
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import java.io.StringReader
import scala.Array

/**
 * Paragraph disambiguator that queries paragraphs once and uses candidate map to filter results.
 * 1) performs candidate selection: searches sf -> uri
 * 2) context score: searches context with filter(uri)
 * 3) disambiguates by ranking candidates found in 1 according to score from 2.
 *
 * @author pablomendes
 */
class TwoStepDisambiguator(val candidateSearcher: CandidateSearcher,
                           val contextSearcher: MergedOccurrencesContextSearcher) //TODO should be ContextSearcher. Need a generic disambiguator to enable this.
    extends ParagraphDisambiguator  {

    SpotlightLog.info(this.getClass, "Initializing disambiguator object ...")

    val disambiguator : Disambiguator = new MergedOccurrencesDisambiguator(contextSearcher)

    SpotlightLog.info(this.getClass, "Done.")

    @throws(classOf[InputException])
    def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence] = {
        // return first from each candidate set
        bestK(paragraph, 5)
            .filter(kv =>
                kv._2.nonEmpty)
            .map( kv =>
                kv._2.head)
            .toList
    }

    //WARNING: this is repetition of BaseSearcher.getHits
    //TODO move to subclass of BaseSearcher
    def query(text: Text, allowedUris: Array[DBpediaResource]) = {
        SpotlightLog.debug(this.getClass, "Setting up query.")

        val context = if (text.text.size<250) text.text.concat(" "+text.text) else text.text //HACK for text that is too short
        //SpotlightLog.debug(this.getClass, context)
        val nHits = allowedUris.size
        val filter = new org.apache.lucene.search.TermsFilter() //TODO can use caching? val filter = new FieldCacheTermsFilter(DBpediaResourceField.CONTEXT.toString,allowedUris)
        allowedUris.foreach( u => filter.addTerm(new Term(DBpediaResourceField.URI.toString,u.uri)) )

        val mlt = new MoreLikeThis(contextSearcher.mReader)
        mlt.setFieldNames(Array(DBpediaResourceField.CONTEXT.toString))
        mlt.setAnalyzer(contextSearcher.getLuceneManager.defaultAnalyzer)
        //SpotlightLog.debug(this.getClass, "Analyzer %s", contextLuceneManager.defaultAnalyzer)
        //val inputStream = new ByteArrayInputStream(context.getBytes("UTF-8"));
        val query = mlt.like(new StringReader(context), DBpediaResourceField.CONTEXT.toString)
        SpotlightLog.debug(this.getClass, "Running query.")
        contextSearcher.getHits(query, nHits, 50000, filter)
    }

    //If you want us to extract allCandidates from occs while we're generating it, you have to pass in the allCandidates parameter
    //If you don't pass anything down, we will just fill in a dummy hashset and let the garbage collector deal with it
    def getCandidates(paragraph: Paragraph, allCandidates: CompactHashSet[DBpediaResource] = CompactHashSet[DBpediaResource]()) = {
        val s1 = System.nanoTime()
        // step1: get candidates for all surface forms (TODO here building allCandidates directly, but could extract from occs)
        val occs = paragraph.occurrences
            .foldLeft( Map[SurfaceFormOccurrence,List[DBpediaResource]]())(
            (acc,sfOcc) => {
                SpotlightLog.debug(this.getClass, "searching...")
                var candidates = new java.util.HashSet[DBpediaResource]().asScala
                try {
                    candidates = candidateSearcher.getCandidates(sfOcc.surfaceForm).asScala //.map(r => r.uri)
                } catch {
                    case e: ItemNotFoundException => SpotlightLog.debug(this.getClass, "%s\n%s", e.getMessage, e.getStackTraceString)
                }
                //ATTENTION there is no r.support at this point
                //TODO if support comes from candidate index, it means c(sf,r).

                //SpotlightLog.debug(this.getClass, "# candidates for: %s = %s (%s)", sfOcc.surfaceForm,candidates.size,candidates)
                SpotlightLog.debug(this.getClass, "# candidates for: %s = %s.", sfOcc.surfaceForm,candidates.size)
                candidates.foreach( r => allCandidates.add(r))
                acc + (sfOcc -> candidates.toList)
            });
        val e1 = System.nanoTime()
        //SpotlightLog.debug(this.getClass, "Time with %s: %f.", m1, (e1-s1) / 1000000.0 )
        occs
    }


    def bestK(paragraph:  Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {

        SpotlightLog.debug(this.getClass, "Running bestK for paragraph %s.", paragraph.id)

        if (paragraph.occurrences.size==0) return Map[SurfaceFormOccurrence,List[DBpediaResourceOccurrence]]()

//        val m1 = if (candLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"
//        val m2 = if (contextLuceneManager.getDBpediaResourceFactory == null) "lucene" else "jdbc"

        // step1: get candidates for all surface forms
        //       (TODO here building allCandidates directly, but could extract from occs)
        var allCandidates = CompactHashSet[DBpediaResource]()
        val occs = getCandidates(paragraph,allCandidates)

        val s2 = System.nanoTime()
        // step2: query once for the paragraph context, get scores for each candidate resource
        var hits : Array[ScoreDoc] = null
        try {
            hits = query(paragraph.text, allCandidates.toArray)
        } catch {
            case e: Exception => throw new SearchException(e)
            case r: RuntimeException => throw new SearchException(r)
            case _ => SpotlightLog.error(this.getClass, "Unknown really scary error happened. You can cry now.")
        }
        // SpotlightLog.debug(this.getClass, "Hits (%d): %s", hits.size, hits.map( sd => "%s=%s".format(sd.doc,sd.score) ).mkString(","))

        // SpotlightLog.debug(this.getClass, "Reading DBpediaResources.")
        val scores = hits
            .foldRight(Map[String,Tuple2[DBpediaResource,Double]]())((hit,acc) => {
            var resource: DBpediaResource = contextSearcher.getDBpediaResource(hit.doc) //this method returns resource.support=c(r)
            var score = hit.score
            //TODO can mix here the scores: c(s,r) / c(r)
            acc + (resource.uri -> (resource,score))
        })
        val e2 = System.nanoTime()
        //SpotlightLog.debug(this.getClass, "Scores (%d): %s", scores.size, scores)

        //SpotlightLog.debug(this.getClass, "Time with %s: %f.".format(m2, (e2-s2) / 1000000.0 )

        // pick the best k for each surface form
        val r = occs.keys.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())( (acc,aSfOcc) => {
            val candOccs = occs.getOrElse(aSfOcc,List[DBpediaResource]())
                    .map( shallowResource => {
                        val (resource: DBpediaResource, supportConfidence: (Int,Double)) = scores.get(shallowResource.uri) match {
                            case Some((fullResource,contextualScore)) => {
                                (fullResource,(fullResource.support,contextualScore))
                            }
                            case _ => (shallowResource,(shallowResource.support,0.0))
                        }
                        Factory.DBpediaResourceOccurrence.from(aSfOcc,
                            resource, //TODO this resource may contain the c(s,r) that can be used for conditional prob.
                            supportConfidence)
                    })
                    .sortBy(o => -o.contextualScore) //TODO should be final score
                .take(k)
            acc + (aSfOcc -> candOccs)
        })

       // SpotlightLog.debug(this.getClass, "Reranked (%d)", r.size)

        r
    }

    def name = {
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