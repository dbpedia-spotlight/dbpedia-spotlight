/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.string

import java.net.{URLDecoder, URLEncoder}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 28.01.11
 * Time: 11:32
 * Partially copied and modified from DBpedia 3.6
 */

object ModifiedWikiUtil {

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
        // Capitalize must be Locale-specific. We must use a different method for languages tr, az, lt.
        // Example: [[istanbul]] generates a link to ?stanbul (dot on the I) on tr.wikipedia.org
        return cleanSpace(URLDecoder.decode(name, "UTF-8"))
    }

    //TODO make code in SurrogatesUtil use this method
    def cleanPageTitle(title: String) = {
       ModifiedWikiUtil.wikiDecode(title)
            .replaceAll(""" \(.+?\)$""", "")
            //.replaceAll("""^(The|THE|A) """, "") //HACK ?
    }

    def cleanDisambiguation(title : String) = {
        title.replaceAll(""" \([D|d]isambiguation\)$""", "")
    }

    // Used by the IndexPriorYSmoother to query the Web for a resource title
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