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

import org.dbpedia.spotlight.string.WikiMarkupStripper
import org.dbpedia.spotlight.model._
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.{MemorySource, WikiPage, Source, XMLSource}
import org.dbpedia.spotlight.log.SpotlightLog
import java.io.{PrintStream, FileOutputStream, File}
import xml.{XML, Elem}
import org.dbpedia.extraction.util.Language

/**
 * Loads Occurrences from a wiki dump.
 */

object WikiOccurrenceSource
{
    // split at paragraphs
    val splitDocumentRegex = """(\n|(<br\s?/?>))(</?\w+?\s?/?>)?(\n|(<br\s?/?>))+"""

    /**
     * Creates an DBpediaResourceOccurrence Source from a dump file.
     */
    def fromXMLDumpFile(dumpFile : File, language: Language) : OccurrenceSource =
    {
        new WikiOccurrenceSource(XMLSource.fromFile(dumpFile, language, _.namespace == Namespace.Main))
    }

    /**
     * Creates an DBpediaResourceOccurrence Source from an XML root element.
     */
    def fromXML(xml : Elem, language: Language) : OccurrenceSource  =
    {
        new WikiOccurrenceSource(XMLSource.fromXML(xml, language))
    }

    /**
     * Creates an DBpediaResourceOccurrence Source from an XML root element string.
     */
    def fromXML(xmlString : String, language: Language) : OccurrenceSource  =
    {
        val xml : Elem = XML.loadString("<dummy>" + xmlString + "</dummy>")  // dummy necessary: when a string "<page><b>text</b></page>" is given, <page> is the root tag and can't be found with the command  xml \ "page"
        new WikiOccurrenceSource(XMLSource.fromXML(xml, language))
    }

  /**
   * Creates a DBpediaResourceOccurrence Source from a Wikipedia heldout paragraph file.
   *
   * @see WikipediaHeldoutCorpus in the eval module
   *
   * @param testFile Iterator of lines containing single MediaWiki paragraphs that
   *                 were extracted as heldout data from the MediaWiki dump.
   * @return
   */
    def fromPigHeldoutFile(testFile: Iterator[String]): OccurrenceSource = {
      new WikiOccurrenceSource(
        new MemorySource(
          testFile.map{ line =>
            new WikiPage(new WikiTitle("Test Paragraph", Namespace.Main, Language.English), line.trim())
          }.toTraversable.asInstanceOf[scala.collection.immutable.Traversable[org.dbpedia.extraction.sources.WikiPage]]
        )
      )
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
                val pageNode = wikiParser( WikiPageUtil.copyWikiPage(wikiPage, cleanSource) )

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
                        SpotlightLog.debug(this.getClass, "Processed %d Wikipedia definition pages (avarage %.2f links per page)", pageCount, occCount/pageCount.toDouble)
                    }
                    if (pageCount %100000 == 0) {
                        SpotlightLog.info(this.getClass, "Processed %d Wikipedia definition pages (avarage %.2f links per page)", pageCount, occCount/pageCount.toDouble)
                    }

                }
            }
        }
    }

    def getOccurrences(paragraph : List[Node], occurrenceIdBase : String) : List[DBpediaResourceOccurrence] =
    {
        var paragraphText = ""

        // collect URIs, surface forms and their offset in this paragraph
        var occurrenceTriples = List[(String, String, Int)]()

        for (node <- paragraph) {
            node match {
                // for text nodes, collect the paragraph text
                case textNode : TextNode => paragraphText += textNode.text

                // for wiki page link nodes collect URI, surface form and offset
                // if the link points to a page in the Main namespace
                case internalLink : InternalLinkNode => {
                    val surfaceFormOffset = paragraphText.length

                    var surfaceForm = internalLink.children.collect { case TextNode(text, _) => WikiMarkupStripper.stripMultiPipe(text) }.mkString("")
                    surfaceForm = surfaceForm.trim.replaceAll(""" \(.+?\)$""", "").replaceAll("""^(The|A) """, "") //TODO should be a filter/transformer instead of hardcoded?

                    paragraphText += surfaceForm

                    if (internalLink.destination.namespace == Namespace.Main && surfaceForm.nonEmpty) {
                        occurrenceTriples ::= new Tuple3(internalLink.destination.encoded, surfaceForm, surfaceFormOffset)
                    }
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