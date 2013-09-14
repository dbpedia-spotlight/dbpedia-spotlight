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

package org.dbpedia.spotlight.util

import org.dbpedia.spotlight.log.SpotlightLog
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.{SpotlightFactory, SpotlightConfiguration, DBpediaResource}

import java.net.{URLEncoder, Socket}
import io.Source
import java.io._
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.config.mappings.DisambiguationExtractorConfig

/**
 * Queries the Web to obtain prior probability counts for each resource
 * The idea is that "huge" will appear much more commonly than "huge TV series" on the Web, even though in Wikipedia that is not the case.
 * This is, thus, an attempt to deal with sparsity in Wikipedia.
 * @author pablomendes
 */

class KeywordExtractor(val configuration: SpotlightConfiguration, val nKeywords : Int = 3) {

    val factory = new SpotlightFactory(configuration)
    val searcher = factory.contextSearcher

    /**
     * Builds a surface form a decoded version of the URI (underscores to spaces, percent codes to chars, etc.)
     * Adds quotes and a plus to indicate this is a MUST query.
     */
    def createKeywordsFromDBpediaResourceURI(resource: DBpediaResource) = {
        def cleanDisambiguation(title : String) = {
            //TODO all languages are taken here; get language from config
            DisambiguationExtractorConfig.disambiguationTitlePartMap.values.foldLeft(title) (
                (acc,s) => title.replaceAll(""" \(%s\)$""".format(s), "")
            )
        }

        val disambiguatedTitle = """(.+?) \((.+?)\)$""".r
        //val simpleTitle = """(.+?)$""".r
        val decoded = WikiUtil.wikiDecode(resource.uri) // remove wikiurl encoded chars
        cleanDisambiguation(decoded) match {
            case disambiguatedTitle(title, explanation) => String.format("+\"%s\" +\"%s\"",title,explanation) // remove parenthesis, add quotes and plus
            case title : String => String.format("+\"%s\"",title)
        }
    }

    /**
     * Extracts top representative terms for this URI to be added to the surface form built from the URI
     * TODO currently based on TF only. Best would be TF*ICF
     */
    def getKeywords(resource: DBpediaResource) = {
        val extraWords = searcher.getContextWords(resource).toList
        SpotlightLog.debug(this.getClass, "Ranked keywords: %s", extraWords.mkString(","))
        extraWords.map( entry => entry.getKey() ).take(nKeywords*2) // get a few extra just in case they overlap with the keywords from the URI
    }

    /**
     * Builds a set of 4 to 10 keywords with which to query the Web
     */
    def getKeywordsWithMust(resource:DBpediaResource) = {
        val keywords = createKeywordsFromDBpediaResourceURI(resource)
        val extraKeywords = getKeywords(resource).filter( w => !keywords.toLowerCase.contains(w.toLowerCase()) ).take(nKeywords).mkString(" ") // remove redundant keywords (those already contained in must clauses)

        keywords+" "+extraKeywords
    }

}

