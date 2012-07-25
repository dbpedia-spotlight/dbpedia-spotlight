package org.dbpedia.spotlight.web.rest

import org.dbpedia.spotlight.lucene.LuceneManager
import java.io.File
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache}
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.extract.LuceneTagExtractor
import org.dbpedia.spotlight.model.{Text, SpotlightConfiguration}

/**
 * Controller object for the /extract interface.
 * TODO This is temporary. Will integrate with /annotate and the others
 * @author pablomendes
 */

object ExtractTags {

    def extractAsJson(text: Text, nHits: Int=100) : String = {
        val values = extractor.extract(text,nHits)
        OutputSerializer.tagsAsJson(text,values)
    }

    def extractAsXml(text: Text, nHits: Int=100) = {
        val values = extractor.extract(text,nHits)
        OutputSerializer.tagsAsXml(text,values)
    }

    val configuration: SpotlightConfiguration = new SpotlightConfiguration("conf/eval.properties")
    val contextIndexDir = LuceneManager.pickDirectory(new File(configuration.getContextIndexDirectory))
    //val contextLuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(contextIndexDir) // use this if all surface forms in the index are lower-cased
    val contextLuceneManager = new LuceneManager.PhoneticSurfaceForms(contextIndexDir) // use this for searches that use phonetic values of strings
    val cache = JCSTermCache.getInstance(contextLuceneManager, configuration.getMaxCacheSize);
    contextLuceneManager.setContextSimilarity(new CachedInvCandFreqSimilarity(cache))        // set most successful Similarity
    contextLuceneManager.setDBpediaResourceFactory(configuration.getDBpediaResourceFactory)
    contextLuceneManager.setDefaultAnalyzer(configuration.getAnalyzer)
    val contextSearcher : MergedOccurrencesContextSearcher = new MergedOccurrencesContextSearcher(contextLuceneManager)

    val extractor = new LuceneTagExtractor(contextLuceneManager,contextSearcher)

}