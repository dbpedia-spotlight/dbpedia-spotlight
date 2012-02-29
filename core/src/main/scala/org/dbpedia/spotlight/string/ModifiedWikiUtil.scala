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

package org.dbpedia.spotlight.string

import java.net.{URLDecoder, URLEncoder}
import org.apache.commons.logging.{LogFactory, Log}

/**
 * Contains code for processing URLs from Wikipedia, performing cleaning, etc.
 * Partially copied and modified from DBpedia 3.6
 * @author maxjakob
 * @author pablomendes - started changing for i18n
 */
object ModifiedWikiUtil {

    val LOG: Log = LogFactory.getLog(this.getClass)

    def cleanSpace( string : String ) : String = {
        return string.replaceAll("_", " ").replaceAll(" +", " ").trim
    }

    def spaceToUnderscore( string : String ) : String = {
        return string.replaceAll(" ", "_")
    }

    def wikiEncode(name : String) : String = {
        // replace spaces by underscores.
        // Note: MediaWiki apparently replaces only spaces by underscores, not other whitespace.
        var encoded = name.replaceAll(" ", "_");

        // normalize duplicate underscores
        encoded = encoded.replaceAll("_+", "_");

        // trim underscores from start
        encoded = encoded.replaceAll("^_", "");

        // trim underscores from end
        encoded = encoded.replaceAll("_$", "");

        // make first character uppercase
        // Capitalize must be Locale-specific. We must use a different method for languages tr, az, lt.
        // Example: [[istanbul]] generates a link to ?stanbul (dot on the I) on tr.wikipedia.org
        encoded = encoded.capitalize

        // URL-encode everything but '/' and ':' - just like MediaWiki
        encoded = URLEncoder.encode(encoded, "UTF-8");
        encoded = encoded.replace("%3A", ":");
        encoded = encoded.replace("%2F", "/");
        encoded = encoded.replace("%26", "&");
        encoded = encoded.replace("%2C", ",");

        return encoded;
    }

    def wikiDecode(name : String) : String =
    {
        var decoded = "";
        // Capitalize must be Locale-specific. We must use a different method for languages tr, az, lt.
        // Example: [[istanbul]] generates a link to ?stanbul (dot on the I) on tr.wikipedia.org
        try {
            decoded = cleanSpace(URLDecoder.decode(name, "UTF-8"))
        } catch {
            case e: java.lang.IllegalArgumentException => {
                LOG.warn("ERROR cannot cleanup SurfaceForm[%s]. WARN kept it as is.\n%s".format(name,e.getStackTraceString));
                decoded = name;
            };
        }
        return decoded;
    }

    def cleanPageTitle(title: String) = {
       ModifiedWikiUtil.wikiDecode(title)
            .replaceAll(""" \(.+?\)$""", "")
            //.replaceAll("""^(The|THE|A) """, "") //HACK ?
    }

    def cleanDisambiguation(title : String) = {
        //TODO LANG move to config file
        // see org.dbpedia.extraction.config.mappings.DisambiguationExtractorConfig
        val disambiguationSuffixes = List(""" \([D|d]isambiguation\)$""", """ \([D|d]esambiguação\)$""", """ \([D|d]esambiguación\)$""");
        disambiguationSuffixes.foldLeft(title)( (acc,s) => title.replaceAll(s, "") )
    }

    // Used by the KeywordExtractor to query the Web for a resource title
    def getKeywordsFromPageTitle(title: String) = {
        val disambiguatedTitle = """(.+?) \((.+?)\)$""".r
        //val simpleTitle = """(.+?)$""".r
        val decoded = ModifiedWikiUtil.wikiDecode(title) // remove wikiurl encoded chars
        cleanDisambiguation(decoded) match {
            case disambiguatedTitle(title, explanation) => String.format("+\"%s\" +\"%s\"",title,explanation) // remove parenthesis, add quotes and plus
            case title : String => String.format("+\"%s\"",title)
        }

    }

    def isEncoded(s : String) =
    {
        // heuristic!!
        if ("""%[0-9a-fA-F][0-9a-fA-F]""".r.findFirstIn(s) != None)
            true
        else
            false
    }

}