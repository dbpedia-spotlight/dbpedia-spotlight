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

import org.dbpedia.spotlight.model.DBpediaResource
import java.net.{Socket, URLEncoder}
import java.io.{BufferedReader, InputStreamReader, PrintStream}
import org.dbpedia.spotlight.log.SpotlightLog


/**
 * This class uses Y!Boss in order to get occurrence estimates for DBpediaURIs (expressed as keyword searches).
 * Needs a configuration file that holds an appID and optionally language and result limit configurations.
 * TODO this class can be generalized to be used to search anything on Y!Boss. For example, to get text containing certain resources, or to get counts. All we need to do is to pass a function value that will be applied inside the while.
 * @author pablomendes (adapted from maxjakob)
 */
class YahooBossSearcher(val webSearchConfig: WebSearchConfiguration) {

    // load configured properties
    val language = webSearchConfig.get("org.dbpedia.spotlight.yahoo.language", "en")
    val region = webSearchConfig.get("org.dbpedia.spotlight.yahoo.region", "us")
    val yahooBossAppID = webSearchConfig.get("org.dbpedia.spotlight.yahoo.appID")
    val yahooBossResults = webSearchConfig.get("org.dbpedia.spotlight.yahoo.maxResults", "50").toInt
    val yahooBossIterations = webSearchConfig.get("org.dbpedia.spotlight.yahoo.maxIterations", "100").toInt

    // define necessary strings for Yahoo! Boss query
    val yahooBossDomain = "boss.yahooapis.com"
    val yahooBossSearchUrl = "/ysearch/web/v1/" //"/ysearch/se_inlink/v1/"
    val appID = "?appid=" + yahooBossAppID
    val options = "&format=xml&type=html&omit_inlinks=domain&strictlang=1&count=" + yahooBossResults + "&lang=" + language + "&Region=" + region

    // Regular expression to parse Y! Boss results and get the counts
    val Results = """\s+<resultset_web count="\d+" start="\d+" totalhits="(\d+)" deephits="(\d+)">""".r

    def get(resource: DBpediaResource, extractor: KeywordExtractor) = {
        val keywords = extractor.getKeywordsWithMust(resource);
        SpotlightLog.info(this.getClass, "Searching for :%s", keywords)
        val results = getYahooAnswer(keywords)
        results
    }

    /**
     * Executes keyword search to Yahoo! Boss  (adapted from WebOccurrenceSource)
     * Answer is a Tuple with two longs representing the total hits and the deep hits for the keywords
     *
     * TODO use HTTPClient and factor this code to a YahooBossSearch class
     */
    def getYahooAnswer(keywords: String) : (Long, Long) =
    {

        val query = yahooBossSearchUrl + URLEncoder.encode(keywords) + appID + options

        // establish connection
        val socket = new Socket("boss.yahooapis.com", 80)
        val printStream = new PrintStream(socket.getOutputStream)
        printStream.print("GET " + query + " HTTP/1.0\r\n")
        printStream.print("Connection: close\r\n\r\n")
        val inReader = new InputStreamReader(socket.getInputStream)
        val buffer = new BufferedReader(inReader)

        var answer : (Long, Long) = (0, 0)
        try {
            // read the answer
            var line = buffer.readLine
            while (line != null)
            {
                //println(line)
                line match {
                    case Results(totalhits, deephits) => {
                        line = null
                        answer = (totalhits.toLong, deephits.toLong)
                        //println(answer)
                        SpotlightLog.info(this.getClass, "Counts: totalhits=%s deephits=%s\n", totalhits, deephits)
                        return answer
                    }
                    case _ => {
                        //print(".");
                        line = buffer.readLine
                    }
                }
            }
        } finally {
            buffer.close
            inReader.close
            printStream.close
            socket.close
        }
        answer // if didn't find hits in the match Results above, return 0 counts
    }
}
