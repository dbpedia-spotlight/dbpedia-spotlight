package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.string.WikiMarkupStripper
import org.dbpedia.spotlight.model._
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.{Source, XMLSource}
import org.apache.commons.logging.LogFactory
import java.io.{PrintStream, FileOutputStream, File}
import xml.{XML, Elem}

/**
 * Loads Occurrences from a wiki dump.
 */

object WikiOccurrenceSource
{
    private val LOG = LogFactory.getLog(this.getClass)

    // split at paragraphs 
    val splitDocumentRegex = """(\n|(<br\s?/?>))(</?\w+?\s?/?>)?(\n|(<br\s?/?>))+"""

    /**
     * Creates an DBpediaResourceOccurrence Source from a dump file.
     */
    def fromXMLDumpFile(dumpFile : File) : OccurrenceSource =
    {
        new WikiOccurrenceSource(XMLSource.fromFile(dumpFile, _.namespace == WikiTitle.Namespace.Main))
    }

    /**
     * Creates an DBpediaResourceOccurrence Source from an XML root element.
     */
    def fromXML(xml : Elem) : OccurrenceSource  =
    {
        new WikiOccurrenceSource(XMLSource.fromXML(xml))
    }

    /**
     * Creates an DBpediaResourceOccurrence Source from an XML root element string.
     */
    def fromXML(xmlString : String) : OccurrenceSource  =
    {
        val xml : Elem = XML.loadString("<dummy>" + xmlString + "</dummy>")  // dummy necessary: when a string "<page><b>text</b></page>" is given, <page> is the root tag and can't be found with the command  xml \ "page"
        new WikiOccurrenceSource(XMLSource.fromXML(xml))
    }

    /**
     * DBpediaResourceOccurrence Source which reads from a wiki pages source.
     */
    private class WikiOccurrenceSource(wikiPages : Source) extends OccurrenceSource
    {
        val wikiParser = WikiParser()

        override def foreach[U](f : DBpediaResourceOccurrence => U) : Unit =
        {
            var pageCount = 0
            var occCount = 0

            for (wikiPage <- wikiPages)
            {
                // clean the wiki markup from everything but links
                val cleanSource = WikiMarkupStripper.stripEverything(wikiPage.source)

                // parse the (clean) wiki page
                val pageNode = wikiParser( wikiPage.copy(source = cleanSource) )

                // exclude redirect and disambiguation pages
                if (!pageNode.isRedirect && !pageNode.isDisambiguation) {

                    // split the page node into paragraphs
                    val paragraphs = NodeUtil.splitNodes(pageNode.children, splitDocumentRegex)
                    var paragraphCount = 0
                    for (paragraph <- paragraphs)
                    {
                        paragraphCount += 1
                        val idBase = pageNode.title.encoded+"-p"+paragraphCount
                        getOccurrences(paragraph, idBase).foreach{occ => occCount += 1
                                                                         f(occ)}
                    }

                    pageCount += 1
                    if (pageCount %5000 == 0) {
                        LOG.debug("Processed %d Wikipedia definition pages (avarage %.2f links per page)".format(pageCount, occCount/pageCount.toDouble))
                    }
                    if (pageCount %100000 == 0) {
                        LOG.info("Processed %d Wikipedia definition pages (avarage %.2f links per page)".format(pageCount, occCount/pageCount.toDouble))
                    }

                }
            }
        }
    }

    def getOccurrences(paragraph : List[Node], occurrenceIdBase : String) : List[DBpediaResourceOccurrence] =
    {
        var paragraphText = ""

        // collect URIs, surface forms and their offset in this paragraph
        var occurrenceTriples = List[Tuple3[String, String, Int]]()

        for (node <- paragraph)
        {
            node match {
                // for text nodes, collect the paragraph text
                case textNode : TextNode => paragraphText += textNode.text

                // for wiki page link nodes collect URI, surface form and offset
                // if the link points to a page in the Main namespace
                case internalLink : InternalLinkNode => {
                    val surfaceFormOffset = paragraphText.length
                    val surfaceForm = internalLink.children.collect {
                                   case TextNode(text, _) => {
                                       val t = WikiMarkupStripper.stripMultiPipe(text)
                                       paragraphText += t
                                       t
                                   }
                    }.mkString("").trim.replaceAll(""" \(.+?\)$""", "").replaceAll("""^(The|A) """, "")
                        if (internalLink.destination.namespace == WikiTitle.Namespace.Main && surfaceForm.nonEmpty)
                            occurrenceTriples ::= new Tuple3(internalLink.destination.encoded, surfaceForm, surfaceFormOffset)
                    }
                case _ =>
            }
        }

        // make a Text instance and check if it is valid
        val textInstance = new Text(paragraphText.replaceAll("""\s""", " "))

        var occurrenceCount = 0
        // make an DBpediaResourceOccurrences
        occurrenceTriples.map{ case (uri : String, sf : String, offset : Int) => {
            occurrenceCount += 1
            val id = occurrenceIdBase + "l" + occurrenceCount
            new DBpediaResourceOccurrence(id, new DBpediaResource(uri), new SurfaceForm(sf), textInstance, offset, Provenance.Wikipedia) }
        }
    }
}