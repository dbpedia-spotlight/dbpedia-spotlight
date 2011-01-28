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


    def isEncoded(s : String) =
    {
        // heuristic!!
        if ("""%[0-9a-fA-F][0-9a-fA-F]""".r.findFirstIn(s) != None)
            true
        else
            false
    }

}