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
import org.dbpedia.spotlight.log.SpotlightLog
import java.io._
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import java.text.ParseException

/**
 * Gets DBpediaResourceOccurrences from TSV files.
 */

object FileOccurrenceSource
{
    /**
     * Creates an DBpediaResourceOccurrence Source from a TSV file.
     */
    def fromFile(tsvFile : File) : OccurrenceSource = new FileOccurrenceSource(tsvFile)

    /**
     * Creates a Definition Source from a TSV file.
     */
    def wikiPageContextFromFile(tsvFile : File) : WikiPageSource = new FileWikiPageSource(tsvFile)

    /**
     * Saves DBpediaResourceOccurrence to a tab-separated file.
     */
    def writeToFile(occSource : Traversable[DBpediaResourceOccurrence], tsvFile : File) {
        var indexDisplay = 0
        SpotlightLog.info(this.getClass, "Writing occurrences to file %s ...", tsvFile)

        var o : OutputStream = new FileOutputStream(tsvFile)
        if (tsvFile.getName.endsWith(".gz")) {
            o = new GZIPOutputStream(o)
        }
        val outStream = new PrintStream(o, true, "UTF-8")

        for (occ <- occSource) {
            outStream.println(occ.toTsvString)

            indexDisplay += 1
            if (indexDisplay % 100000 == 0) {
                SpotlightLog.info(this.getClass, "  saved %d occurrences", indexDisplay)
            }
        }
        outStream.close

        SpotlightLog.info(this.getClass, "Finished: saved %d occurrences to file", indexDisplay)
    }


    /**
     * Saves WikipediaDefinitions to a tab-separated file.
     */
    def addToFile(defSource : WikiPageSource, tsvFile : File) {
        var indexDisplay = 0
        SpotlightLog.info(this.getClass, "Writing wiki page text to file %s ...", tsvFile)

        var o : OutputStream = new FileOutputStream(tsvFile)
        if (tsvFile.getName.endsWith(".gz")) {
            o = new GZIPOutputStream(o)
        }
        val outStream = new PrintStream(o, true, "UTF-8")

        for (definition <- defSource) {
            outStream.println(definition.toTsvString)

            indexDisplay += 1
            if (indexDisplay % 100000 == 0) {
                SpotlightLog.info(this.getClass, "  saved %d wiki page texts", indexDisplay)
            }
        }
        outStream.close

        SpotlightLog.info(this.getClass, "Finished: saved %d wiki page texts to file", indexDisplay)
    }

    /**
     * DBpediaResourceOccurrence Source from previously saved data.
     */
    private class FileOccurrenceSource(tsvFile : File) extends OccurrenceSource {

        override def foreach[U](f : DBpediaResourceOccurrence => U) {

            var input : InputStream = new FileInputStream(tsvFile)
            if (tsvFile.getName.endsWith(".gz")) {
                input = new GZIPInputStream(input)
            }

            //something fishy going on here:
            // if you get a java.nio.charset.UnmappableCharacterException:
            //     put "UTF-8" as second argument of fromInputStream
            // if you get a java.nio.charset.MalformedInputException:
            //     call fromInputStream only with one argument
            var linesIterator : Iterator[String] = Iterator.empty
            try {
                linesIterator = Source.fromInputStream(input, "UTF-8").getLines
            }
            catch {
                case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(input).getLines
            }

            for (line <- linesIterator) {
                val elements = line.trim.split("\t")

                if (elements.length == 5) {
                    val id = elements(0)
                    val res = new DBpediaResource(elements(1), 1) // support is at least one if this resource has been seen once here
                    val sf = new SurfaceForm(elements(2))
                    val t = new Text(elements(3))
                    val offset = elements(4).toInt

                    f( new DBpediaResourceOccurrence(id, res, sf, t, offset, Provenance.Wikipedia) )
                }
                else {
                    //throw new ParseException("line must have 4 tab separators; got "+(elements.length-1)+" in line: "+line, elements.length-1)
                    SpotlightLog.error(this.getClass, "line must have 4 tab separators; got %d in line: %d", elements.length-1, line)
                }

            }
        }
    }

    private class FileWikiPageSource(tsvFile : File) extends WikiPageSource
    {
        override def foreach[U](f : WikiPageContext => U) : Unit =
        {
            var input : InputStream = new FileInputStream(tsvFile)
            if (tsvFile.getName.endsWith(".gz")) {
                input = new GZIPInputStream(input)
            }

            for (line <- Source.fromInputStream(input, "UTF-8").getLines) {
                try {
                    val elements = line.trim.split("\t")
                    if (elements.length == 2)
                    {
                        val res = new DBpediaResource(elements(0), 1)
                        val definitionText = new Text(elements(1))

                        val pageContext = new WikiPageContext(res, definitionText)
                        f(pageContext)
                    }
                }
                catch {
                    case err : Exception => {System.err.println(line);System.err.println(err.getClass + "\n" + err.getMessage)}
                }
            }
        }
    }
}