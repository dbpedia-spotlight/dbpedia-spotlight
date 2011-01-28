package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model._
import io.Source
import org.apache.commons.logging.LogFactory
import java.io._
import java.util.zip.{GZIPOutputStream, GZIPInputStream}


/**
 * Gets DBpediaResourceOccurrences from TSV files.
 */

object FileOccurrenceSource
{
    private val LOG = LogFactory.getLog(this.getClass)

    /**
     * Creates an DBpediaResourceOccurrence Source from a TSV file.
     */
    def fromFile(tsvFile : File) : OccurrenceSource =
    {
        new FileOccurrenceSource(tsvFile)
    }

    /**
     * Creates a Definition Source from a TSV file.
     */
    def wikiPageContextFromFile(tsvFile : File) : WikiPageSource =
    {
        new FileWikiPageSource(tsvFile)
    }

    /**
     * Saves DBpediaResourceOccurrence to a tab-separated file.
     */
    def addToFile(occSource : OccurrenceSource, tsvFileName : String)
    {
        var indexDisplay = 0
        LOG.info("Adding occurrences to file "+tsvFileName+" ...")

        val outStream = new PrintStream((if (tsvFileName.endsWith(".gz")) new GZIPOutputStream(new FileOutputStream(tsvFileName, true))
                                         else new FileOutputStream(tsvFileName, true)), true, "UTF-8")

        for (occ <- occSource)
        {
            outStream.println(occ.toTsvString)

            indexDisplay += 1
            if (indexDisplay % 100000 == 0)
                LOG.debug("  saved " + indexDisplay + " occurrences")
        }
        outStream.close

        LOG.info("Finished: saved " + indexDisplay + " occurrences to file")
    }

    def addToFile(occSource : OccurrenceSource, tsvFile : File) {
        addToFile(occSource, tsvFile.getAbsolutePath)
    }

    /**
     * Saves WikipediaDefinitions to a tab-separated file.
     */
    def addToFile(defSource : WikiPageSource, tsvFile : File)
    {
        var indexDisplay = 0
        LOG.info("Adding definitions to file '" + tsvFile.getPath + "' ...")

        val outStream = new PrintStream((if (tsvFile.getName.endsWith(".gz")) new GZIPOutputStream(new FileOutputStream(tsvFile, true))
                                         else new FileOutputStream(tsvFile, true)), true, "UTF-8")

        for (wikiDef <- defSource)
        {
            outStream.println(wikiDef.resource.uri + "\t" + wikiDef.context.text)

            indexDisplay += 1
            if (indexDisplay % 10000 == 0)
                LOG.debug("  saved " + indexDisplay + " definitions")
        }
        outStream.close

        LOG.info("Finished: saved " + indexDisplay + " definitions to file")
    }

    /**
     * DBpediaResourceOccurrence Source from previously saved data.
     */
    private class FileOccurrenceSource(tsvFile : File) extends OccurrenceSource
    {
        override def foreach[U](f : DBpediaResourceOccurrence => U) : Unit =
        {
            var input : InputStream = new FileInputStream(tsvFile)
            if (tsvFile.getName.endsWith(".gz")) {
                input = new GZIPInputStream(input)
            }

            //TODO something fishy going on here:
            //TODO PABLO Pablo thinks it's char encoding problems. Try resaving the file as UTF-8 and things may get solved.
            // if you get a java.nio.charset.UnmappableCharacterException:
            //     put "UTF-8" as second argument of fromInputStream
            // if you get a java.nio.charset.MalformedInputException:
            //     call fromInputStream only with one argument

            //for (line <- Source.fromInputStream(input).getLines)
            for (line <- Source.fromInputStream(input, "UTF-8").getLines)
            {
                val elements = line.trim.split("\t")
                if (elements.length == 6)
                {
                    val id = elements(0)
                    val res = new DBpediaResource(elements(1))
                    val sf = new SurfaceForm(elements(2))
                    val t = new Text(elements(3))
                    val offset = -1 //elements(4).toInt
                    res.types = elements(5).split(",").toList.map(new DBpediaType(_))

                    f( new DBpediaResourceOccurrence(id, res, sf, t, offset, Provenance.Wikipedia) )
                } else if (elements.length == 5) {
                    val id = elements(0)
                    val res = new DBpediaResource(elements(1))
                    val sf = new SurfaceForm(elements(2))
                    val t = new Text(elements(3))
                    val offset = -1 //elements(4).toInt

                    f( new DBpediaResourceOccurrence(id, res, sf, t, offset, Provenance.Wikipedia) )
                } else if (elements.length == 4) {
                    val res = new DBpediaResource(elements(0))
                    val sf = new SurfaceForm(elements(1))
                    val t = new Text(elements(2))
                    val offset = -1 //elements(3).toInt

                    f( new DBpediaResourceOccurrence(res, sf, t, offset, Provenance.Wikipedia) )
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

            for (line <- Source.fromInputStream(input, "UTF-8").getLines)
            {
                try {
                    val elements = line.trim.split("\t")
                    if (elements.length == 2)
                    {
                        val res = new DBpediaResource(elements(0))
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