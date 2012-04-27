/*
 * Copyright 2011 DBpedia Spotlight Development Team
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
import collection.JavaConversions._
import org.dbpedia.spotlight.annotate.{DefaultAnnotator, DefaultParagraphAnnotator}
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.lucene.disambiguate.MergedOccurrencesDisambiguator
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy

/**
 * This class contains many of the "defaults" for DBpedia Spotlight.
 * Maybe consider renaming to DefaultFactory or DBpediaSpotlightController
 *
 * @author pablomendes
 */
class SpotlightFactory(val configuration: SpotlightConfiguration) {

    private val LOG = LogFactory.getLog(this.getClass)

    val analyzer = configuration.analyzer
    assert(analyzer!=null)

    val directory : Directory = LuceneManager.pickDirectory(new File(configuration.getContextIndexDirectory))
    val luceneManager : LuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
    val similarity : Similarity = new CachedInvCandFreqSimilarity(JCSTermCache.getInstance(luceneManager, configuration.getMaxCacheSize))

    val lingPipeFactory : LingPipeFactory = new LingPipeFactory(new File(configuration.getTaggerFile), new IndoEuropeanSentenceModel())

    luceneManager.setDefaultAnalyzer(analyzer);
    luceneManager.setContextSimilarity(similarity);

    // The dbpedia resource factory is used every time a document is retrieved from the index.
    // We can use the index itself as provider, or we can use a database. whichever is faster.
    // If the factory is left null, BaseSearcher will use Lucene. Otherwise, it will use the factory.
    val dbpediaResourceFactory : DBpediaResourceFactory = configuration.getDBpediaResourceFactory
    luceneManager.setDBpediaResourceFactory(dbpediaResourceFactory)

    val searcher = new MergedOccurrencesContextSearcher(luceneManager);

    val spotters = new java.util.HashMap[SpotterConfiguration.SpotterPolicy,Spotter]()
    val disambiguators = new java.util.HashMap[SpotlightConfiguration.DisambiguationPolicy,ParagraphDisambiguatorJ]()

    //populate
    LOG.info("Initiating spotters...")
    spotter()
    LOG.info("Initiating disambiguators...")
    disambiguator()
    LOG.info("Done.")

    def spotter(policy: SpotterConfiguration.SpotterPolicy) : Spotter = {
        if (policy == SpotterConfiguration.SpotterPolicy.Default) {
            spotters.getOrElse(policy, spotter(SpotterConfiguration.SpotterPolicy.LingPipeSpotter)); // if no default, use lingpipe
        } else if(policy == SpotterConfiguration.SpotterPolicy.LingPipeSpotter) {
            spotters.getOrElse(policy, new LingPipeSpotter(new File(configuration.getSpotterConfiguration.getSpotterFile)))
        } else if (policy == SpotterConfiguration.SpotterPolicy.AtLeastOneNounSelector) {
            spotters.getOrElse(policy, SpotterWithSelector.getInstance(spotter(SpotterConfiguration.SpotterPolicy.LingPipeSpotter),new AtLeastOneNounSelector(),taggedTokenProvider()))
        } else if (policy == SpotterConfiguration.SpotterPolicy.CoOccurrenceBasedSelector) {
            spotters.getOrElse(policy, SpotterWithSelector.getInstance(spotter(SpotterConfiguration.SpotterPolicy.LingPipeSpotter),new CoOccurrenceBasedSelector(configuration.getSpotterConfiguration, taggedTokenProvider()), taggedTokenProvider()))
        } else if (policy == SpotterConfiguration.SpotterPolicy.NESpotter) {
            spotters.getOrElse(policy, new NESpotter(configuration.getSpotterConfiguration.getOpenNLPModelDir))
        } else if (policy == SpotterConfiguration.SpotterPolicy.WikiMarkupSpotter) {
            new WikiMarkupSpotter
        } else {
            new WikiMarkupSpotter
        }
    }

    def spotter() : Spotter = {
        val spotterPolicies = configuration.getSpotterConfiguration.getSpotterPolicies
        spotterPolicies.foreach( policy => {
            spotters.put(policy, spotter(policy))
        })
        val default = spotter(spotterPolicies.get(0)) // default is first in configuration list
        spotters.put(SpotterPolicy.Default, default)
        default
    }

    def disambiguator() : ParagraphDisambiguatorJ = {
        val disambiguatorPolicies = configuration.getDisambiguatorConfiguration.getDisambiguatorPolicies
        disambiguatorPolicies.foreach( policy => {
            disambiguators.put(policy, disambiguator(policy))
        })
        val default = disambiguator(disambiguatorPolicies.get(0)) // default is first in configuration list
        disambiguators.put(DisambiguationPolicy.Default,default)
        default
    }

    def disambiguator(policy: SpotlightConfiguration.DisambiguationPolicy) : ParagraphDisambiguatorJ = {
        if (policy == SpotlightConfiguration.DisambiguationPolicy.Default) {
            disambiguator(SpotlightConfiguration.DisambiguationPolicy.Occurrences)
        } else if (policy == SpotlightConfiguration.DisambiguationPolicy.Document) {
            disambiguators.getOrElse(policy, new ParagraphDisambiguatorJ(new TwoStepDisambiguator(this)))
        } else if (policy == SpotlightConfiguration.DisambiguationPolicy.Occurrences) {
            disambiguators.getOrElse(policy, new ParagraphDisambiguatorJ(new DefaultDisambiguator(this)))
        } else if (policy == SpotlightConfiguration.DisambiguationPolicy.CuttingEdge) {
            disambiguators.getOrElse(policy, new ParagraphDisambiguatorJ(new CuttingEdgeDisambiguator(this)))
        } else { // by default use Occurrences
            disambiguators.getOrElse(SpotlightConfiguration.DisambiguationPolicy.Occurrences, new ParagraphDisambiguatorJ(new DefaultDisambiguator(this)))
        }
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
}