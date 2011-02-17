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

package org.dbpedia.spotlight.io

import org.cyberneko.html.parsers.DOMParser
import org.dbpedia.spotlight.model._
import java.io._
import io.Source
import org.apache.commons.logging.LogFactory
import java.util.zip.GZIPInputStream
import java.net.{Socket, URL}
import org.xml.sax.InputSource
import org.dbpedia.spotlight.util.{OccurrenceFilter, IndexConfiguration}

/**
 * Gets Occurrences from the Web using Yahoo! Boss.
 */
object WebOccurrenceSource
{
    private val LOG = LogFactory.getLog(this.getClass)

    val blacklist = Set("wikipedia.org",
                        "spacecoast-trading.ipower.com",
                        "photography-now.net",
                        "www.kosmix.com",
                        "www.biographicon.com",
                        "www.beat-wings.com")

    // load configured properties
    val language = IndexConfiguration.get("Language", "en")
    val region = IndexConfiguration.get("Region", "us")
    val yahooBossAppID = IndexConfiguration.get("YahooBossAppID", "vwl9D_PV34FEFsrjr_QByFTJfD6ahlU77i3NF8if986Dy2dSzI8eC71XuP6Ui_FAPHBPekqCEXQ-")
    val yahooBossResults = IndexConfiguration.get("YahooBossResults", "50").toInt
    val yahooBossIterations = IndexConfiguration.get("YahooBossIterations", "100").toInt

    // define necessary strings for Yahoo! Boss query
    val wikipediaPrefixEn = "http://" + language + ".wikipedia.org/wiki/"
    val yahooBossDomain = "boss.yahooapis.com"
    val yahooBossSearchUrl = "/ysearch/se_inlink/v1/"
    val appID = "?appid=" + yahooBossAppID
    val options = "&format=xml&type=html&omit_inlinks=domain&strictlang=1&count=" + yahooBossResults + "&lang=" + language + "&Region=" + region

    val yahooBossOutLinkUrlRegex = """<url>([^<]+?)</url>""".r

    val timeOut = 500  // in milliseconds


    /**
     * Create an DBpediaResourceOccurrence Source for a set of DBpediaResources.
     * Only the first n web occurrences of each resource are produced.
     */
    def forResources(resources : Iterable[DBpediaResource], n : Int = Integer.MAX_VALUE) : OccurrenceSource =
    {
        new YahooBossSource(resources, n)
    }

    /**
     * DBpediaResourceOccurrence Source which queries Yahoo Boss and retrieves links to Wikipedia pages.
     * Then finds the occurrence of that link in the respective page and paragraph.
     */
    private class YahooBossSource(val resources : Iterable[DBpediaResource], val n : Int) extends OccurrenceSource
    {
        val parser = new DOMParser
        //val occFilter = IndexConfiguration.occurrenceFilter
        val occFilter = new OccurrenceFilter(maximumSurfaceFormLength = 40, minimumParagraphLength = 50, maximumParagraphLength = 500)

        override def foreach[U](f : DBpediaResourceOccurrence => U) : Unit =
        {
            for (resource <- resources)
            {
                // TODO this is ugly but most efficient; find a pretty and efficient way
                var iterationCount = 1
                var occCount = 0
                var yahooOutLinks : Iterator[URL] = List(new URL("http://dummy.com")).toIterator
                var bossAnswerXml = getYahooAnswer(resource, iterationCount*yahooBossResults)

                val x = 1
                while (occCount < n && iterationCount < yahooBossIterations && !(yahooOutLinks.isEmpty))
                {
                    LOG.info("retrieved " + (iterationCount+1)*yahooBossResults + " search results for " + resource + " ...")

                    // get URLs that contain links to Wikipedia pages
                    yahooOutLinks = yahooBossOutLinkUrlRegex.findAllIn(bossAnswerXml).matchData.map{m => new URL(m.subgroups(0))}
                    for (link <- yahooOutLinks.filter{l => !((l.getHost endsWith ".pdf") || (blacklist contains l.getHost))})
                    {
                        // build an occurrence from a page, described by a URL
                        getOccurrenceFromUrl(link, resource) match {
                            case Some(occ : DBpediaResourceOccurrence) =>  { occCount +=1 ; f(occ) }
                            case None =>
                        }
                    }
                    bossAnswerXml = getYahooAnswer(resource, iterationCount*yahooBossResults)
                    iterationCount += 1
                }
            }
        }

        /**
         * Returns an occurrence from a web page.
         */
        private def getOccurrenceFromUrl(url : URL, resource : DBpediaResource) : Option[DBpediaResourceOccurrence] =
        {
            LOG.info("Connecting to " + url.toString)

            val urlConnection = url.openConnection
            urlConnection.setReadTimeout(timeOut)

            // build the a Document
            try {
                parser.parse(new InputSource(urlConnection.getInputStream))
            }
            catch {
                case ioe : IOException => LOG.info("  ERROR retrieving page"); return None
            }

            val targetUrl = wikipediaPrefixEn + resource.uri
            
            // get the links on the page
            val links = parser.getDocument.getElementsByTagName("A")  // elements must be upper-case

            // get list of link elements that point to the target Wikipedia article
            val targetLinks = (0 to links.getLength-1).map(links.item(_)).filter{ link =>
                val href = link.getAttributes.getNamedItem("href")  // attributes must be lower-case
                href != null && href.getTextContent == targetUrl
            }
            if (targetLinks.isEmpty)
                LOG.debug("    no link found to '" + targetUrl + "'")

            for (link <- targetLinks)
            {
                // get, normalize and test all occurrence data
                val surfaceForm = new SurfaceForm(link.getTextContent)
                var parent = link.getParentNode

                // go up the document elements until a <P> or a <DIV> tag
                while (!(Set("P", "DIV", "LI", "HTML") contains parent.getNodeName))  // elements must be upper-case
                    parent = parent.getParentNode

                // alternatively, go up only one level, diving through <B> and <I>
                //if (Set("B", "I") contains parent.getNodeName)
                //    parent = parent.getParentNode

                val context = new Text(cleanParagraph(parent.getTextContent))
                if (!(parent.getNodeName.equals("HTML")) && occFilter.isGoodText(context))
                {
                    val offset = context.text.indexOf(surfaceForm.name)
                    val occ = new DBpediaResourceOccurrence(url.toString, resource, surfaceForm, context, offset, Provenance.Web)

                    // return only if it is certain that the offset is set to the correct occurrence
                    if (context.text.indexOf(surfaceForm.name, offset+1) == -1 && occFilter.isGoodOccurrence(occ)) 
                    {
                        LOG.info("  OK, occurrence created: " + occ)
                        // returns the only the first occurrence of a specific resource in a web page
                        return Some(occ)
                    }
                    else { LOG.debug("    not a good occurrence: " + occ) }
                }
                else { LOG.debug("    not a good paragraph: " + context) }
            }
            LOG.info("  FAIL, no occurrence created")
            None
        }

        private def cleanParagraph(paragraph : String) : String =
        // TODO: test this
        {
            paragraph.replaceAll("""\w\w\w\w?\w?://\S+?""", "")  // erase displayed links
                     .replaceAll("""[\n|\t]""", " ")             // erase line breaks and tabs
                     .replaceAll("""\s\s+""", " ")               // erase whitespace
                     .trim
        }

        /**
         * Returns a HTTP answer from Yahoo! Boss for a URL and a position where the results start.
         */
        private def getYahooAnswer(resource : DBpediaResource,  start : Int) : String =
        {
            val targetUrl = wikipediaPrefixEn + resource.uri
            val query = yahooBossSearchUrl + targetUrl + appID + options + "&start=" + start

            // establish connection
            val socket = new Socket("boss.yahooapis.com", 80)
            val printStream = new PrintStream(socket.getOutputStream)
            printStream.print("GET " + query + " HTTP/1.0\r\n")
            printStream.print("Connection: close\r\n\r\n")
            val inReader = new InputStreamReader(socket.getInputStream)
            val buffer = new BufferedReader(inReader)

            // read the answer
            var answer = ""
            var line = buffer.readLine
            while (line != null)
            {
                answer += line +"\n"
                line = buffer.readLine
            }

            buffer.close
            inReader.close
            printStream.close
            socket.close

            answer
        }
    }



    def main(args : Array[String])
    {
        val resourcesFileName = args(0)
        val outputFileName = args(1)
        val resourceCountLimit = args(2).toInt
        val sampleNumber = args(3).toInt    // for each resource
        val startURI = if (args.length == 5) args(4) else ""   // specify last seen resource befor dying

        LOG.info("Building occurrences for "+resourceCountLimit+" resources ("+sampleNumber+" samples each)")
        LOG.info("  Getting resources from "+resourcesFileName+" ...")

        var input : InputStream = new FileInputStream(new File(resourcesFileName))
        if (resourcesFileName.toLowerCase.endsWith(".gz")) input = new GZIPInputStream(input)
        val linesIterator = Source.fromInputStream(input, "UTF-8").getLines

        var resourceCount = 0
        var collect = false
        while (resourceCount < resourceCountLimit)
        {
            val line = linesIterator.next.trim
            val targetResource = new DBpediaResource(line)

            if (collect) {
                LOG.info("** Resource: "+targetResource)
                try {
                    val occSource = forResources(List(targetResource), sampleNumber)
                    FileOccurrenceSource.writeToFile(occSource, new File(outputFileName))
                }
                catch {
                    case e : Exception => LOG.warn("AN EXCEPTION OCCURRED!!! Ignoring: "+e)
                }
                resourceCount += 1
            }

            if ((startURI equals "") || (startURI equals targetResource.uri))
                collect = true
        }

        LOG.info("  Done.")
    }

}