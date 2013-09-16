package org.dbpedia.spotlight.io

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

import org.cyberneko.html.parsers.DOMParser
import org.dbpedia.spotlight.model._
import java.io._
import io.Source
import org.dbpedia.spotlight.log.SpotlightLog
import java.util.zip.GZIPInputStream
import java.net.{Socket, URL}
import org.xml.sax.InputSource
import org.dbpedia.spotlight.util.{IndexingConfiguration}

/**
 * Gets Occurrences from the Web using Yahoo! Boss.
 * @author maxjakob
 */
object WebOccurrenceSource
{
    val blacklist = Set("wikipedia.org",
                        "spacecoast-trading.ipower.com",
                        "photography-now.net",
                        "www.kosmix.com",
                        "www.biographicon.com",
                        "www.beat-wings.com")

    val yahooBossOutLinkUrlRegex = """<url>([^<]+?)</url>""".r

    val timeOut = 500  // in milliseconds


    /**
     * Create an DBpediaResourceOccurrence Source for a set of DBpediaResources.
     * Only the first n web occurrences of each resource are produced.
     */
    def forResources(indexingConfig: IndexingConfiguration, resources : Iterable[DBpediaResource], n : Int = Integer.MAX_VALUE) : OccurrenceSource =
    {
        new YahooBossSource(indexingConfig, resources, n)
    }

    /**
     * DBpediaResourceOccurrence Source which queries Yahoo Boss and retrieves links to Wikipedia pages.
     * Then finds the occurrence of that link in the respective page and paragraph.
     */
    private class YahooBossSource(val indexingConfig: IndexingConfiguration, val resources : Iterable[DBpediaResource], val n : Int) extends OccurrenceSource
    {

        // load configured properties
        val language = indexingConfig.get("org.dbpedia.spotlight.yahoo.language", "en")
        val region = indexingConfig.get("org.dbpedia.spotlight.yahoo.region", "us")
        val yahooBossAppID = indexingConfig.get("org.dbpedia.spotlight.yahoo.appID")
        val yahooBossResults = indexingConfig.get("org.dbpedia.spotlight.yahoo.maxResults", "50").toInt
        val yahooBossIterations = indexingConfig.get("org.dbpedia.spotlight.yahoo.maxIterations", "100").toInt

        // define necessary strings for Yahoo! Boss query
        val wikipediaPrefixEn = "http://" + language + ".wikipedia.org/wiki/"
        val yahooBossDomain = "boss.yahooapis.com"
        val yahooBossSearchUrl = "/ysearch/se_inlink/v1/"
        val appID = "?appid=" + yahooBossAppID
        val options = "&format=xml&type=html&omit_inlinks=domain&strictlang=1&count=" + yahooBossResults + "&lang=" + language + "&Region=" + region


        val parser = new DOMParser
        //val occFilter = IndexConfiguration.occurrenceFilter
        val occFilter = new MonolithicOccurrenceFilter(maximumSurfaceFormLength = 40, minimumParagraphLength = 50, maximumParagraphLength = 500)

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
                    SpotlightLog.info(this.getClass, "retrieved %d search results for %s ...", (iterationCount+1)*yahooBossResults, resource)

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
            SpotlightLog.info(this.getClass, "Connecting to %s", url.toString)

            val urlConnection = url.openConnection
            urlConnection.setReadTimeout(timeOut)

            // build the a Document
            try {
                parser.parse(new InputSource(urlConnection.getInputStream))
            }
            catch {
                case ioe : IOException => SpotlightLog.info(this.getClass, "  ERROR retrieving page"); return None
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
                SpotlightLog.debug(this.getClass, "    no link found to '%s'", targetUrl)

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
                        SpotlightLog.info(this.getClass, "  OK, occurrence created: %s", occ)
                        // returns the only the first occurrence of a specific resource in a web page
                        return Some(occ)
                    }
                    else { SpotlightLog.debug(this.getClass, "    not a good occurrence: %s", occ) }
                }
                else { SpotlightLog.debug(this.getClass, "    not a good paragraph: %s", context) }
            }
            SpotlightLog.info(this.getClass, "  FAIL, no occurrence created")
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
        val indexingConfigFileName = args(0)
        val config = new IndexingConfiguration(indexingConfigFileName)

        val resourcesFileName = args(1)
        val outputFileName = args(2)
        val resourceCountLimit = args(3).toInt
        val sampleNumber = args(4).toInt    // for each resource
        val startURI = if (args.length == 6) args(5) else ""   // specify last seen resource befor dying

        SpotlightLog.info(this.getClass, "Building occurrences for %d resources (%d samples each)", resourceCountLimit, sampleNumber)
        SpotlightLog.info(this.getClass, "  Getting resources from %s ...", resourcesFileName)

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
                SpotlightLog.info(this.getClass, "** Resource: %s", targetResource)
                try {
                    val occSource = forResources(config, List(targetResource), sampleNumber)
                    FileOccurrenceSource.writeToFile(occSource, new File(outputFileName))
                }
                catch {
                    case e : Exception => SpotlightLog.warn(this.getClass, "AN EXCEPTION OCCURRED!!! Ignoring: %s", e)
                }
                resourceCount += 1
            }

            if ((startURI equals "") || (startURI equals targetResource.uri))
                collect = true
        }

        SpotlightLog.info(this.getClass, "  Done.")
    }

}


import org.dbpedia.spotlight.model.{Text, DBpediaResource, SurfaceForm, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.string.ContextExtractor

/**
 * Filters sources of occurrences and WikiPageContext so that they contain only "good" data.
 *
 * Usage:
 * val occFilter = IndexConfiguration.occurrenceFilter
 * for (cleanOccurrence <- occFilter.filter(someOccurrenceSource)) {
 *     // process
 * }
 *
 */

@deprecated("please use org.dbpedia.spotlight.filter.occurrences.OccurrenceFilter!")
class MonolithicOccurrenceFilter(val maximumSurfaceFormLength : Int = Integer.MAX_VALUE,
                       val minimumParagraphLength : Int = 0,
                       val maximumParagraphLength : Int = Integer.MAX_VALUE,
                       val redirectsTC : Map[String,String] = Map.empty,
                       val conceptURIs : Set[String] = Set.empty,
                       //var surfaceForms : Map[String,List[String]] = Map[String,List[String]](),  // uri -> List(Sfs)
                       val contextExtractor : ContextExtractor = null,
                       val lowerCaseSurfaceForms : Boolean = false)
{

    /**
     * Returns an Occurrence source that is free of "bad" occurrences.
     */
    def filter(occurrenceSource : OccurrenceSource) : OccurrenceSource =
    {
        new FilteredOccurrenceSource(occurrenceSource)
    }

//    /**
//      * Disregard surface forms that do not obey the configuration constraints.
//      */
//    def isGoodSurfaceFormForResource(sf : String, uri : String) : Boolean =
//    {
//        var sfString = sf
//        var validSurfaceForms = surfaceForms.get(uri).getOrElse(List[String]())
//        if (lowerCaseSurfaceForms) {
//            sfString = sf.toLowerCase
//            validSurfaceForms = validSurfaceForms.map(_.toLowerCase)
//        }
//        (sf.length <= maximumSurfaceFormLength) && (surfaceForms.nonEmpty && (validSurfaceForms contains sfString))
//    }
//
//    def isGoodSurfaceFormForResource(sf : SurfaceForm, res : DBpediaResource) : Boolean =
//    {
//        isGoodSurfaceFormForResource(sf.name, res.uri)
//    }

    /**
      * Disregard links to URIs that do not obey the configuration constraints.
      */
    def isGoodURI(uri : String) : Boolean =
    {
        conceptURIs.nonEmpty && (conceptURIs contains uri)
    }

    def isGoodResource(res : DBpediaResource) : Boolean =
    {
        isGoodURI(res.uri)
    }

    /**
      * Disregard Texts that do not obey the configuration constraints.
      */
    def isGoodText(textString : String) : Boolean =
    {
        if (textString.length < minimumParagraphLength || textString.length > maximumParagraphLength)
            return false

        true
    }
    def isGoodText(text : Text) : Boolean =
    {
        isGoodText(text.text)
    }

    /**
      * Disregard Occurrences that do not obey the configuration constraints.
      */
    def isGoodOccurrence(occ : DBpediaResourceOccurrence) : Boolean =
    {
        if (!isGoodText(occ.context))
            return false

        if (!isGoodResource(occ.resource))
            return false

//        if (!isGoodSurfaceFormForResource(occ.surfaceForm, occ.resource))
//            return false

        //if (occ.textOffset < 0)
        //   return false

        true
    }

    /**
     * If the URI refers to a redirect page, follow the redirect chain until the end to the resource URI
     * and return a new DBpediaResourceOccurrence.
     */
    def resolveRedirects(occ : DBpediaResourceOccurrence) : DBpediaResourceOccurrence = {
        redirectsTC.get(occ.resource.uri) match {
            case Some(uri) => {
                val resolvedResource = new DBpediaResource(uri)
                new DBpediaResourceOccurrence(occ.id, resolvedResource, occ.surfaceForm, occ.context, occ.textOffset, occ.provenance)
            }
            case None => occ
        }
    }


    /**
     * Wrapper class that applies all filters when iterating.
     */
    private class FilteredOccurrenceSource(occurrenceSource : OccurrenceSource) extends OccurrenceSource {

        override def foreach[U](f : DBpediaResourceOccurrence => U) {

            for (occ <- occurrenceSource) {

                var thisOcc = resolveRedirects(occ)

//                // make title surface form if the found surface form is not allowed (too noisy) for this URI
//                if (!isGoodSurfaceFormForResource(thisOcc.surfaceForm, thisOcc.resource)) {
//                    val titleAsSurfaceForm = SurrogatesUtil.getSurfaceForm(thisOcc.resource)
//                    thisOcc = new DBpediaResourceOccurrence(occ.id, occ.resource, titleAsSurfaceForm, occ.context, -1, occ.provenance)
//                }

                if (contextExtractor != null) {
                    thisOcc = contextExtractor.narrowContext(thisOcc)
                }

                if (isGoodOccurrence(thisOcc)) {
                    if (lowerCaseSurfaceForms) {
                        thisOcc = new DBpediaResourceOccurrence(thisOcc.id, thisOcc.resource, new SurfaceForm(thisOcc.surfaceForm.name.toLowerCase), thisOcc.context, thisOcc.textOffset, thisOcc.provenance)
                    }

                    f( thisOcc )
                }
            }
        }
    }

}