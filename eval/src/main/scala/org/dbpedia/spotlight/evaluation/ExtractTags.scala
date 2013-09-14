package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache}
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import java.io.File
import org.dbpedia.spotlight.string.WikiLinkParser
import io.Source
import org.dbpedia.spotlight.model.{Text, SpotlightConfiguration}
import org.dbpedia.spotlight.extract.LuceneTagExtractor

/**
 * Evaluation class for LuceneTagExtractor
 * @author pablomendes
 */

object ExtractTags {

    def main(args: Array[String]) {
        val configuration = new SpotlightConfiguration(args(0))
//        val baseDir = args(1)
//        val nTags = if (args.size > 2) args(2) else 100

        val nTags = 250
        val baseDir: String = "/home/pablo/eval/bbc/"
        val inputFile: File = new File(baseDir+"gold/transcripts.txt");
        val outputDir = baseDir+"spotlight/Spotlight"

        SpotlightLog.info(this.getClass, "Initializing disambiguator object ...")

        val contextIndexDir = LuceneManager.pickDirectory(new File(configuration.getContextIndexDirectory))
        //val contextLuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(contextIndexDir) // use this if all surface forms in the index are lower-cased
        val contextLuceneManager = new LuceneManager.PhoneticSurfaceForms(contextIndexDir) // use this for searches that use phonetic values of strings
        val cache = JCSTermCache.getInstance(contextLuceneManager, configuration.getMaxCacheSize);
        contextLuceneManager.setContextSimilarity(new CachedInvCandFreqSimilarity(cache))        // set most successful Similarity
        contextLuceneManager.setDBpediaResourceFactory(configuration.getDBpediaResourceFactory)
        contextLuceneManager.setDefaultAnalyzer(configuration.getAnalyzer)
        val contextSearcher : MergedOccurrencesContextSearcher = new MergedOccurrencesContextSearcher(contextLuceneManager)

        val extractor = new LuceneTagExtractor(contextLuceneManager,contextSearcher)

        val plainText = Source.fromFile(inputFile).mkString

        var i = 0;
        for (text <- plainText.split("\n\n")) {
            i = i+1;
            try {
                val cleanText = WikiLinkParser.eraseMarkup(text);
                SpotlightLog.info(this.getClass, "Doc %d", i)
                SpotlightLog.info(this.getClass, "Doc length: %s tokens", cleanText.split(" ").size)
                val tags = extractor.extract(new Text(cleanText),nTags)
                EvalUtils.print(outputDir+"Tags",tags,"doc"+i)
            } catch {
                case e: Exception =>
                    SpotlightLog.error(this.getClass, "Exception: %s", e)
            }

        }


    }

}