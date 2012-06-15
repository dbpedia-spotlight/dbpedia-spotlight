package org.dbpedia.spotlight.disambiguate

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

import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import java.io.File
import org.dbpedia.spotlight.lucene.similarity._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.exceptions.{SearchException, InputException}
import org.apache.lucene.search.Explanation
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.lucene.disambiguate.{MergedOccurrencesDisambiguator, MixedWeightsDisambiguator}
import java.lang.UnsupportedOperationException
import scalaj.collection.Imports._
import collection.mutable.{HashMap, HashSet}
import org.dbpedia.spotlight.graph.AdjacencyList
import io.Source
import org.semanticweb.yars.nx.Node

/**
 * Uses a graph derived from DBpedia to rescore candidates based on how close they are to a perceived topical center.
 * Initial idea for this center is the top scored entity.
 * - Multiple centers are possible.
 * - Multiple graphs are possible
 * - Graphs where nodes are vectors and edges are the cosine between vectors are an idea
 *
 * @author pablomendes
 */
class GraphCentralityDisambiguator(val configuration: SpotlightConfiguration) extends ParagraphDisambiguator  {

    private val LOG = LogFactory.getLog(this.getClass)


    //val dataset = "mappingbased_properties"
    //val dataset = "article_categories"
    //val dataset = "page_links"
    val dataset = "mappingbased_with_categories"

    val triplesFile = new File("/data/dbpedia/en/"+dataset+"_en.nt")     // read from one disk

    //LOG.info("Reading entity set");
    //val entities = Source.fromFile("/home/pablo/eval/csaw/original/entity.set").toSet;
    //val validSubjects = entities.map(s => new DBpediaResource(s.toString).uri).toSet;

    def isValidSubject(triple: Array[Node]) = {
        AdjacencyList.isObjectProperty(triple)// && validSubjects.contains(new DBpediaResource(triple(1).toString).uri)
    }

    LOG.info("Initializing adjacency matrix ...")
    AdjacencyList.load(triplesFile, isValidSubject)

    LOG.info("Initializing disambiguator object ...")

    val indexDir = new File(configuration.getContextIndexDirectory)

    // Disambiguator
    val dir = LuceneManager.pickDirectory(indexDir)

    //val luceneManager = new LuceneManager(dir)                              // use this if surface forms in the index are case-sensitive
    val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(dir)  // use this if all surface forms in the index are lower-cased
    val cache = JCSTermCache.getInstance(luceneManager, configuration.getMaxCacheSize);
    luceneManager.setContextSimilarity(new CachedInvCandFreqSimilarity(cache))        // set most successful Similarity

    val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager)

    val disambiguator : Disambiguator = new MergedOccurrencesDisambiguator(contextSearcher)

    LOG.info("Done.")

    @throws(classOf[InputException])
    def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence] = {
        // return first from each candidate set
        bestK(paragraph, 5).map( kv => kv._2(0) ).toList
    }

    def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]= {
        val sfOccurrences = paragraph.occurrences

        if (sfOccurrences.size==0) return Map[SurfaceFormOccurrence,List[DBpediaResourceOccurrence]]()

        var centralEntity = new DBpediaResourceOccurrence(null,null,null,-1,java.lang.Double.MIN_VALUE);
        val centralEntities = new HashSet[DBpediaResourceOccurrence]();
        var candidateSet = new HashSet[DBpediaResource](); //list or set? //TODO a potential problem with a list here is that a sfOcc that occurs many times can trump all the others even if its contextual score is low
        val candidateMap = sfOccurrences.foldLeft(Map[SurfaceFormOccurrence,List[DBpediaResourceOccurrence]]()){
         (acc,sfOcc) => {
            val candidates = disambiguator.bestK(sfOcc, k).asScala
            val topCandidate = candidates(0)
            // adding one central entity per surface form
            centralEntities.add(topCandidate)
            // determine one centralEntity for paragraph
            if (topCandidate.contextualScore > centralEntity.contextualScore) centralEntity = topCandidate
            // build candidateSet
            candidates.foreach(c => candidateSet.add(c.resource))
            acc + (sfOcc -> candidates.toList)
         }
        }

        // compute distance from centralEntity to candidateSet (this step could have been precomputed)
        def distance(a: DBpediaResource, b: DBpediaResource) = AdjacencyList.intersect(a.uri,b.uri,"1hop").length.toDouble

        // score each candidate resource according to distance
        def getScores(currentMap: Map[DBpediaResource,Double], candidateSet: HashSet[DBpediaResource], centralEntity: DBpediaResourceOccurrence) = {
            candidateSet.foldLeft(currentMap){
                (acc, c) => {
                    val d : Double = distance(centralEntity.resource, c)
                    if (d>0) LOG.debug("%s -> %s = %f".format(centralEntity.resource, c, d))
                    val dNew : Double = acc.getOrElse(c, 0.0) + d
                    acc + (c -> dNew)
                }
            }
        }

        // for one central entity
        //val scoredCandidateSet = getScores(Map[DBpediaResource,Double](), candidateSet, centralEntity)

        // for a central entity per surface form
        val scoredCandidateSet = centralEntities.foldLeft(Map[DBpediaResource,Double]()){ (acc, centEnt) => getScores(acc, candidateSet, centEnt) }

        // rescore candidateMap
        val rescoredCandidateMap = candidateMap.foldLeft(Map[SurfaceFormOccurrence,List[DBpediaResourceOccurrence]]())(
            (acc, kv) => {
                val sfOcc = kv._1
                val candidates = kv._2
                candidates.foreach( c => {
                    val gScore = scoredCandidateSet.getOrElse(c.resource,1.0)
                    c.similarityScore = c.contextualScore * gScore
                })
                //   rerank candidates
                acc + (sfOcc -> candidates.sortBy(_.similarityScore).toList)
            })
        rescoredCandidateMap
    }


    def name() : String = {
        "GCD+"+disambiguator.name
    }

    def ambiguity(sf : SurfaceForm) : Int = {
        disambiguator.ambiguity(sf)
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