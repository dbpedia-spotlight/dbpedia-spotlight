package org.dbpedia.spotlight.extract

import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import org.apache.lucene.search.similar.MoreLikeThis
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache}
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.string.WikiLinkParser
import java.io.{PrintStream, StringReader, File}
import weka.classifiers.evaluation.EvaluationUtils

/**
 * TagExtractor implementation that uses Lucene's MoreLikeThisQuery to return related tags.
 */
class LuceneTagExtractor(val contextLuceneManager: LuceneManager, val contextSearcher: MergedOccurrencesContextSearcher) extends TagExtractor {

    private val LOG = LogFactory.getLog(this.getClass)

    def extract(text: Text, nTags: Int) : Seq[(DBpediaResource,Double)] = {
        getRelatedResources(text).map( hit => {
            val resource = contextSearcher.getDBpediaResource(hit.doc, Array(LuceneManager.DBpediaResourceField.URI.toString))
            val score = hit.score.toDouble
            (resource, score)
        })
    }

    //WARNING: this is repetition of BaseSearcher.getHits
    //TODO move to subclass of BaseSearcher
    def getRelatedResources(text: Text, nHits: Int = 100) = {
        LOG.debug("Setting up query.")

        var context = if (text.text.size<250) text.text.concat(" "+text.text) else text.text //HACK for text that is too short
        context = context.replaceAll("_"," ") //HACK for Andreas' class names

        val filter = null

        val mlt = new MoreLikeThis(contextSearcher.mReader);
        mlt.setFieldNames(Array(DBpediaResourceField.CONTEXT.toString))
        mlt.setAnalyzer(contextLuceneManager.defaultAnalyzer)
        //LOG.debug("Analyzer %s".format(contextLuceneManager.defaultAnalyzer))
        //val inputStream = new ByteArrayInputStream(context.getBytes("UTF-8"));
        val query = mlt.like(new StringReader(context), DBpediaResourceField.CONTEXT.toString);
        LOG.debug("Running query.")
        contextSearcher.getHits(query, nHits, 50000, filter)
    }

}