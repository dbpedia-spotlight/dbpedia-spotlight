/*
 * Copyright 2011 Pablo Mendes, Max Jakob
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

package org.dbpedia.spotlight.model

import org.apache.lucene.util.Version
import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.exceptions.ConfigurationException
import org.apache.lucene.store.Directory
import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.search.Similarity
import org.dbpedia.spotlight.lucene.similarity.{JCSTermCache, CachedInvCandFreqSimilarity}
import com.aliasi.sentences.IndoEuropeanSentenceModel
import org.dbpedia.spotlight.disambiguate._
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import java.io.File
import org.dbpedia.spotlight.spot._
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters
import org.dbpedia.spotlight.tagging.lingpipe.{LingPipeTextUtil, LingPipeTaggedTokenProvider, LingPipeFactory}
import collection.mutable.HashMap
import collection.JavaConversions._
import org.dbpedia.spotlight.annotate.{DefaultAnnotator, DefaultParagraphAnnotator}
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.lucene.disambiguate.MergedOccurrencesDisambiguator

/**
 * This class contains many of the "defaults" for DBpedia Spotlight. Maybe consider renaming to DefaultFactory.
 *
 * @author pablomendes
 */
class SpotlightFactory(val configuration: SpotlightConfiguration,
                    val analyzer: Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET)
                    ) {

    private val LOG = LogFactory.getLog(this.getClass)

    def this(configuration: SpotlightConfiguration) {
        this(configuration, new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", configuration.getStopWords))
        if (!new File(configuration.getTaggerFile).exists()) throw new ConfigurationException("POS tagger file does not exist! "+configuration.getTaggerFile);
    }

    val directory : Directory = LuceneManager.pickDirectory(new File(configuration.getContextIndexDirectory))
    val luceneManager : LuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
    val similarity : Similarity = new CachedInvCandFreqSimilarity(JCSTermCache.getInstance(luceneManager, configuration.getMaxCacheSize))

    val lingPipeFactory : LingPipeFactory = new LingPipeFactory(new File(configuration.getTaggerFile), new IndoEuropeanSentenceModel())

    luceneManager.setDefaultAnalyzer(analyzer);
    luceneManager.setContextSimilarity(similarity);

    // The dbpedia resource factory is used every time a document is retrieved from the index.
    // We can use the index itself as provider, or we can use a database. whichever is faster.
    val dbpediaResourceFactory : DBpediaResourceFactory = configuration.getDBpediaResourceFactory
    luceneManager.setDBpediaResourceFactory(dbpediaResourceFactory)
    LOG.debug("DBpedia Resource Factory is null?? %s".format(luceneManager.getDBpediaResourceFactory == null))

    val searcher = new MergedOccurrencesContextSearcher(luceneManager);

    val spotters = new java.util.HashMap[SpotterConfiguration.SpotterPolicy,Spotter]()
    val disambiguators = new java.util.HashMap[SpotlightConfiguration.DisambiguationPolicy,ParagraphDisambiguatorJ]()

    //populate
    spotter()
    disambiguator()

    def disambiguator() : ParagraphDisambiguatorJ = {
        //configuration.getDisambiguationConfiguration.getDisambiguationPolicies
        SpotlightConfiguration.DisambiguationPolicy.values().foreach( policy => {
            disambiguators.put(policy, disambiguator(policy))
        })
        disambiguators.head._2
    }

    def disambiguator(name: SpotlightConfiguration.DisambiguationPolicy) : ParagraphDisambiguatorJ = { //TODO define enum
        if (name == SpotlightConfiguration.DisambiguationPolicy.Document) {
            disambiguators.getOrElse(name, new ParagraphDisambiguatorJ(new TwoStepDisambiguator(configuration)))
        } else if (name == SpotlightConfiguration.DisambiguationPolicy.Occurrences) {
            disambiguators.getOrElse(name, new ParagraphDisambiguatorJ(new DefaultDisambiguator(configuration)))
        } else if (name == SpotlightConfiguration.DisambiguationPolicy.CuttingEdge) {
            disambiguators.getOrElse(name, new ParagraphDisambiguatorJ(new CuttingEdgeDisambiguator(configuration)))
        } else {
            disambiguators.getOrElse(name, new ParagraphDisambiguatorJ(new DefaultDisambiguator(configuration)))
        }
    }

    def spotter(policy: SpotterConfiguration.SpotterPolicy) : Spotter = {
        if (policy == SpotterConfiguration.SpotterPolicy.LingPipeSpotter)
            spotters.getOrElse(policy, new LingPipeSpotter(new File(configuration.getSpotterConfiguration.getSpotterFile)))
        else if (policy == SpotterConfiguration.SpotterPolicy.AtLeastOneNounSelector) {
            spotters.getOrElse(policy, SpotterWithSelector.getInstance(spotter(SpotterConfiguration.SpotterPolicy.LingPipeSpotter),new AtLeastOneNounSelector(),taggedTokenProvider()))
        } else if (policy == SpotterConfiguration.SpotterPolicy.CoOccurrenceBasedSelector) {
            spotters.getOrElse(policy, SpotterWithSelector.getInstance(spotter(SpotterConfiguration.SpotterPolicy.LingPipeSpotter),new CoOccurrenceBasedSelector(configuration.getSpotterConfiguration),taggedTokenProvider()))
        } else {
            new WikiMarkupSpotter
        }
    }

    def spotter() : Spotter = {
        configuration.getSpotterConfiguration.getSpotterPolicies.foreach( policy => {
            spotters.put(policy, spotter(policy))
        })
        spotters.head._2
    }

    def annotator() ={
        new DefaultAnnotator(spotter(), new MergedOccurrencesDisambiguator(searcher))
        //new DefaultParagraphAnnotator(spotter(), disambiguator())
    }

    def filter() ={
        new CombineAllAnnotationFilters(configuration)
    }

    def taggedTokenProvider() = {
       new LingPipeTaggedTokenProvider(lingPipeFactory);
    }

    def textUtil() = {
       new LingPipeTextUtil(lingPipeFactory);
    }

    object DBpediaResource {
        def from(dbpediaID : String) : DBpediaResource = dbpediaResourceFactory.from(dbpediaID)
        def from(dbpediaResource : DBpediaResource) = dbpediaResourceFactory.from(dbpediaResource.uri)
    }

}