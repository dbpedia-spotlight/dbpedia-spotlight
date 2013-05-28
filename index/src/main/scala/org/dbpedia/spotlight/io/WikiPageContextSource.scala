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

import java.io.File
import xml.Elem
import org.dbpedia.extraction.sources.{Source, XMLSource}
import org.dbpedia.spotlight.string.WikiMarkupStripper
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.spotlight.model.{DBpediaResource, Text, WikiPageContext}
import org.dbpedia.extraction.util.Language

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 05-Jul-2010
 * Time: 10:32:58
 * To change this template use File | Settings | File Templates.
 */


object WikiPageContextSource
{
    /**
     * Creates an DBpediaResourceOccurrence Source from a dump file.
     */
    def fromXMLDumpFile(dumpFile : File, language: Language) : WikiPageSource =
    {
        new WikipediaPageContextSource(XMLSource.fromFile(dumpFile, language, _.namespace == Namespace.Main))
    }

    /**
     * Creates an DBpediaResourceOccurrence Source from an XML root element.
     */
    def fromXML(xml : Elem, language: Language) : WikiPageSource =
    {
        new WikipediaPageContextSource(XMLSource.fromXML(xml, language))
    }

    /**
     * DBpediaResourceOccurrence Source which reads from a wiki pages source.
     */
    private class WikipediaPageContextSource(wikiPages : Source) extends WikiPageSource
    {
        val wikiParser = WikiParser()

        override def foreach[U](f : WikiPageContext => U) : Unit =
        {
            for (wikiPage <- wikiPages)
            {
                // clean the wiki markup from everything but links
                val cleanSource = WikiMarkupStripper.stripEverything(wikiPage.source)

                // parse the (clean) wiki page
                val pageNode = wikiParser( WikiPageUtil.copyWikiPage(wikiPage, cleanSource) )

                // exclude redirects, disambiguation pages and other undesired pages (e.g. Lists)
                if (!pageNode.isRedirect && !pageNode.isDisambiguation)
                {
                    val pageContext = new Text( getPageText(pageNode) )
                    val resource = new DBpediaResource(pageNode.title.encoded)
                    f( new WikiPageContext(resource, pageContext) )
                }
            }
        }
    }

    def getPageText(node : Node) : String =
    {
        node.children.map{
            _ match
            {
                case textNode : TextNode => WikiMarkupStripper.stripMultiPipe(textNode.text.trim)
                case internalLink : InternalLinkNode => { getPageText(internalLink) }
                case _ => ""
            }
        }.mkString(" ").replaceAll("""\n""", " ").replaceAll("""\s""", " ")
    }
        

}