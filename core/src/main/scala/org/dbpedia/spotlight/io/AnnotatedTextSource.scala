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

import org.dbpedia.spotlight.model._
import io.Source
import java.io._
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import tools.nsc.doc.model.comment.Paragraph
import org.apache.commons.lang.NotImplementedException
import scala.collection.JavaConversions._
import java.util.ArrayList
import org.junit.Test
import java.text.ParseException

/**
 * Reads *SORTED* DBpediaResourceOccurrences from TSV files and converts them to paragraphs that hold N occurrences.
 * It assumes sorting by paragraph (or occurrence id when those two orders are equivalent).
 * This is in contrast with @link{OccurrenceSource}, which holds one resource+text per object.
 * This is useful for applications that want to look at relationships between occurrences in the same paragraph
 *
 * @author pablomendes
 */
trait AnnotatedTextSource extends Traversable[AnnotatedParagraph] {
    def name : String = "AnnotatedTextSource"
}

object AnnotatedTextSource {

    def fromOccurrencesString(text: String) : AnnotatedTextSource = {
        new TSVOccurrencesSortedByText(text.split("\n").iterator)
    }

    /**
     * Creates an DBpediaResourceOccurrence Source from a TSV file containing <id,uri,sf,text>
     */
    def fromOccurrencesFile(tsvFile : File) : AnnotatedTextSource = {
        var input : InputStream = new FileInputStream(tsvFile)
        if (tsvFile.getName.endsWith(".gz")) {
            input = new GZIPInputStream(input)
        }

        var linesIterator : Iterator[String] = Iterator.empty
        try {
            linesIterator = Source.fromInputStream(input, "UTF-8").getLines
        }
        catch {
            case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(input).getLines
        }

        new TSVOccurrencesSortedByText(linesIterator)
    }

    /**
     * Saves DBpediaResourceOccurrence to a tab-separated file.
     */
    def writeToFile(occSource : OccurrenceSource, tsvFile : File) {
        throw new NotImplementedException("Need to adapt method from object FileOccurrenceSource");
    }

    /**
     * Saves WikipediaDefinitions to a tab-separated file.
     */
    def addToFile(defSource : WikiPageSource, tsvFile : File) {
        throw new NotImplementedException("Need to adapt method from object FileOccurrenceSource");
    }

    /**
     * Builds paragraphs from DBpediaResourceOccurrences saved as TSV
     */
    private class TSVOccurrencesSortedByText(linesIterator: Iterator[String]) extends AnnotatedTextSource {

        // This method assumes sorting of occurrences by paragraph (or occurrence id when they are equivalent)
        override def foreach[U](f : AnnotatedParagraph => U) {

            var currentText = new Text("")
            var occs = List[DBpediaResourceOccurrence]()
            var i = 0;
            for (line <- linesIterator) {
                i = i + 1
                val elements = line.trim.split("\t")
                if (elements.length == 5) {
                    val id = elements(0)
                    val res = new DBpediaResource(elements(1))
                    val sf = new SurfaceForm(elements(2))
                    val t = new Text(elements(3).trim())
                    val offset = elements(4).toInt
                    val occ = new DBpediaResourceOccurrence(id, res, sf, t, offset, Provenance.Wikipedia)
                    if (currentText.text == "" || t.text == currentText.text) {
                        occs = occ :: occs
                    } else {
                        f( new AnnotatedParagraph(currentText, occs) )
                        occs = List(occ)
                    }
                    currentText = t
                }
                else {
                    throw new IOException("line must have 5 tab separated fields; got %d in line %d: %s".format(elements.length,i,line))
                }
            }
            f( new AnnotatedParagraph(currentText, occs) ) // for the last occurrence
        }
    }

    /**
     * Builds paragraphs from DBpediaResourceOccurrences saved as TSV, sorted by ID (used in TAC KBP for example)
     */
    private class TSVOccurrencesSortedById(linesIterator: Iterator[String]) extends AnnotatedTextSource {

        // This method assumes sorting of occurrences by id
        override def foreach[U](f : AnnotatedParagraph => U) {

            var currentId = ""
            var currentText = ""
            var occs = List[DBpediaResourceOccurrence]()
            var i = 0;
            for (line <- linesIterator) {
                i = i + 1
                val elements = line.trim.split("\t")
                if (elements.length == 5) {
                    val id = elements(0)
                    val res = new DBpediaResource(elements(1))
                    val sf = new SurfaceForm(elements(2))
                    val t = new Text(elements(3).trim())
                    val offset = elements(4).toInt
                    val occ = new DBpediaResourceOccurrence(id, res, sf, t, offset, Provenance.Wikipedia)
                    if (currentId == "" || id == currentId) {
                        occs = occ :: occs
                    } else {
                        f( new AnnotatedParagraph(currentId, new Text(currentText), occs) )
                        occs = List(occ)
                    }
                    currentId = id
                    currentText = currentText.concat(t.text)
                }
                else {
                    throw new IOException("line must have 5 tab separated fields; got %d in line %d: %s".format(elements.length,i,line))
                }
            }
            f( new AnnotatedParagraph(currentId, new Text(currentText), occs) ) // for the last occurrence
        }
    }


}
