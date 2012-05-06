package org.dbpedia.spotlight.disambiguate

import com.officedepot.cdap2.collection.CompactHashSet._
import com.officedepot.cdap2.collection.CompactHashSet
import org.dbpedia.spotlight.model.{Text, DBpediaResource, Paragraph}
import org.apache.lucene.index.Term
import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import org.apache.lucene.search.similar.MoreLikeThis
import scala.Array._
import java.io.ByteArrayInputStream

/**
 * A TagExtractor is almost like a Disambiguator, but it does not constrain the tags to things that were *mentioned* in text.
 * A concept/entity that was not mentioned but is related should also be returned.
 *
 * @author pablomendes
 */

class TagExtractor {

    //WARNING: this is repetition of BaseSearcher.getHits
    //TODO move to subclass of BaseSearcher
    def getRelevantResources(text: Text, nHits: Int = 200) = {
        LOG.debug("Setting up query.")

        val context = if (text.text.size<250) text.text.concat(" "+text.text) else text.text //HACK for text that is too short
        //LOG.debug(context)

        val (filter,nHits) = if (allowedUris.size>0) { // allow only URIs passed in the list of allowed
            val filter = new org.apache.lucene.search.TermsFilter() //TODO can use caching? val filter = new FieldCacheTermsFilter(DBpediaResourceField.CONTEXT.toString,allowedUris)
            allowedUris.foreach( u => filter.addTerm(new Term(DBpediaResourceField.URI.toString,u.uri)) )
            (filter, allowedUris.size)
        } else {  // no URIs were passed in, allow by default the top 200
            (null, 200) //TODO should be configurable
        }

        val mlt = new MoreLikeThis(contextSearcher.mReader);
        mlt.setFieldNames(Array(DBpediaResourceField.CONTEXT.toString))
        mlt.setAnalyzer(contextLuceneManager.defaultAnalyzer)
        //LOG.debug("Analyzer %s".format(contextLuceneManager.defaultAnalyzer))
        val inputStream = new ByteArrayInputStream(context.getBytes("UTF-8"));
        val query = mlt.like(inputStream, DBpediaResourceField.CONTEXT.toString);
        LOG.debug("Running query.")
        contextSearcher.getHits(query, nHits, 50000, filter)
    }

}