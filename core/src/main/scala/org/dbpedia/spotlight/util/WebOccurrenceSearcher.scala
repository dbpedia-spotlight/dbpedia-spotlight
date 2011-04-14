package org.dbpedia.spotlight.util

import org.dbpedia.spotlight.string.ModifiedWikiUtil
import org.apache.commons.logging.LogFactory
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.{LuceneFactory, SpotlightConfiguration, DBpediaResource}

import org.dbpedia.spotlight.util.WebSearchConfiguration
import java.net.{URLEncoder, Socket}
import io.Source
import java.io._

/**
 * Queries the Web to obtain prior probability counts for each resource
 * The idea is that "huge" will appear much more commonly than "huge TV series" on the Web, even though in Wikipedia that is not the case.
 * This is, thus, an attempt to deal with sparsity in Wikipedia.
 * TODO can  be generalized -- see YahooBossSearcher.getYahooAnswer()
 * @author pablomendes
 */

object WebOccurrenceSearcher {

    private val LOG = LogFactory.getLog(this.getClass)

    val configuration = new SpotlightConfiguration("conf/eval.properties"); //TODO move this to command line parameter

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
        LOG.info(String.format("Ranked keywords: %s", extraWords.mkString(",")))
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

    /**
     * This class uses Y!Boss in order to get occurrence estimates for DBpediaURIs (expressed as keyword searches).
     * Needs a configuration file that holds an appID and optionally language and result limit configurations.
     * TODO this class can be generalized to be used to search anything on Y!Boss. For example, to get text containing certain resources, or to get counts. All we need to do is to pass a function value that will be applied inside the while.
     */
    private class YahooBossSearcher(val webSearchConfig: WebSearchConfiguration) {

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

        def get(resource: DBpediaResource) = {
            val keywords = getAllKeywords(resource);
            LOG.info("Searching for :"+keywords)
            val results = getYahooAnswer(keywords)
            results
        }

        /**
         * Executes keyword search to Yahoo! Boss  (adapted from WebOccurrenceSource)
         * Answer is a Tuple with two longs representing the total hits and the deep hits for the keywords
         *
         * TODO use HTTPClient and factor this code to a YahooBossSearch class
         */
        private def getYahooAnswer(keywords: String) : (Long, Long) =
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
                            LOG.info(String.format("Counts: totalhits=%s deephits=%s\n", totalhits, deephits))
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


    /**
     * This class obtains DBpediaResource Web occurrence counts.
     * It uses a configuration file with parameters for the Yahoo!Boss search API, and takes as input a file with one URI per line.
     *
     */
    def main(args: Array[String]) {

        val indexingConfigFileName = args(0)
        val uriSetFile = args(1)
        val config = new WebSearchConfiguration(indexingConfigFileName);
        val prior = new YahooBossSearcher(config)

        val out = new PrintWriter(uriSetFile+".counts");
        val uriSet = Source.fromFile(uriSetFile).getLines
        var i = 0;
        uriSet.foreach( uri => {
            i = i + 1
            LOG.info(String.format("URI %s : %s", i.toString, uri));
            val results = prior.get(new DBpediaResource(uri))
            out.print(String.format("%s\t%s\t%s\n", uri, results._1.toString, results._2.toString))
            if (i % 100 == 0) {
                out.flush
            } else {
                Thread.sleep(700)
            }
        });

        out.close
    }

}

