package org.dbpedia.spotlight.extract

import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import org.apache.lucene.search.similar.MoreLikeThis
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache}
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.string.WikiLinkParser
import java.io.{PrintStream, StringReader, File}
import weka.classifiers.evaluation.EvaluationUtils
import org.apache.lucene.search._
import scala.collection.JavaConversions._
import org.apache.lucene.index.Term
import java.util

/**
 * TagExtractor implementation that uses Lucene's MoreLikeThisQuery to return related tags.
 */
class LuceneTagExtractor(val contextLuceneManager: LuceneManager, val contextSearcher: MergedOccurrencesContextSearcher) extends TagExtractor {

    def extract(text: Text, nTags: Int) : Seq[(DBpediaResource,Double)] = {
        extract(text, nTags, List[OntologyType]())
    }

    def extract(text: Text, nTags: Int, ontologyTypes: List[OntologyType]) : Seq[(DBpediaResource,Double)] = {
        val fields = Array(LuceneManager.DBpediaResourceField.URI.toString, LuceneManager.DBpediaResourceField.URI_COUNT.toString, LuceneManager.DBpediaResourceField.TYPE.toString)
        getRelatedResources(text,nTags,ontologyTypes).map( hit => {
            val resource = contextSearcher.getDBpediaResource(hit.doc, fields)
            val score = hit.score.toDouble
            (resource, score)
        })
    }

    //WARNING: this is repetition of BaseSearcher.getHits
    //TODO move to subclass of BaseSearcher
    def getRelatedResources(text: Text, nHits: Int = 100, ontologyTypes: java.util.List[OntologyType]) = {
        SpotlightLog.debug(this.getClass, "Setting up query.")

        var context = if (text.text.size<250) text.text.concat(" "+text.text) else text.text //HACK for text that is too short
        context = context.replaceAll("_"," ") //HACK for Andreas' class names

        val typesQuery = new BooleanQuery()
        ontologyTypes.foreach( t => typesQuery.add(new TermQuery(new Term(DBpediaResourceField.TYPE.toString(),t.typeID)), BooleanClause.Occur.SHOULD) )
        val filter = if (ontologyTypes.size()>0) new CachingWrapperFilter(new QueryWrapperFilter(typesQuery)) else null

        val mlt = new MoreLikeThis(contextSearcher.mReader);
        mlt.setFieldNames(Array(DBpediaResourceField.CONTEXT.toString))
        mlt.setAnalyzer(contextLuceneManager.defaultAnalyzer)
        //SpotlightLog.debug(this.getClass, "Analyzer %s", contextLuceneManager.defaultAnalyzer)
        //val inputStream = new ByteArrayInputStream(context.getBytes("UTF-8"));
        val query = mlt.like(new StringReader(context), DBpediaResourceField.CONTEXT.toString);
        //val filteredQuery: FilteredQuery = new FilteredQuery(query, new CachingWrapperFilter(new QueryWrapperFilter(typesQuery)))
        SpotlightLog.debug(this.getClass, "Running query.")
        contextSearcher.getHits(query, nHits, 50000, filter)
    }

}