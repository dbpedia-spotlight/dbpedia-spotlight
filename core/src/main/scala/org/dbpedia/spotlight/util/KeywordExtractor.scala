package org.dbpedia.spotlight.util

import org.dbpedia.spotlight.string.ModifiedWikiUtil
import org.apache.commons.logging.LogFactory
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.{LuceneFactory, SpotlightConfiguration, DBpediaResource}

import java.net.{URLEncoder, Socket}
import io.Source
import java.io._

/**
 * Queries the Web to obtain prior probability counts for each resource
 * The idea is that "huge" will appear much more commonly than "huge TV series" on the Web, even though in Wikipedia that is not the case.
 * This is, thus, an attempt to deal with sparsity in Wikipedia.
 * @author pablomendes
 */

class KeywordExtractor(val configuration: SpotlightConfiguration) {

    private val LOG = LogFactory.getLog(this.getClass)

    val factory = new LuceneFactory(configuration)
    val searcher = factory.searcher

    val nKeywords = 3;

    /**
     * Builds a surface form a decoded version of the URI (underscores to spaces, percent codes to chars, etc.)
     * Adds quotes and a plus to indicate this is a MUST query.
     */
    def createKeywordsFromDBpediaResourceURI(resource: DBpediaResource) = {
        ModifiedWikiUtil.getKeywordsFromPageTitle(resource.uri);
    }

    /**
     * Extracts top representative terms for this URI to be added to the surface form built from the URI
     * TODO currently based on TF only. Best would be TF*ICF
     */
    def augmentKeywords(resource: DBpediaResource) = {
        val extraWords = searcher.getContextWords(resource).toList
        LOG.debug(String.format("Ranked keywords: %s", extraWords.mkString(",")))
        extraWords.map( entry => entry.getKey() ).take(nKeywords*2) // get a few extra just in case they overlap with the keywords from the URI
    }

    /**
     * Builds a set of 4 to 10 keywords with which to query the Web
     */
    def getAllKeywords(resource:DBpediaResource) = {
        val keywords = createKeywordsFromDBpediaResourceURI(resource)
        val extraKeywords = augmentKeywords(resource).filter( w => !keywords.toLowerCase.contains(w.toLowerCase()) ).take(nKeywords).mkString(" ") // remove redundant keywords (those already contained in must clauses)

        keywords+" "+extraKeywords
    }

}

