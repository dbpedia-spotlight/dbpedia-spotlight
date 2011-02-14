package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.util.OccurrenceFilter
import io.Source
import java.io.File
import org.dbpedia.spotlight.io.{AllOccurrenceSource, FileOccurrenceSource, DisambiguationContextSource, WikiOccurrenceSource}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.string.ContextExtractor

/**
 * Saves Occurrences to a TSV file.
 * - Surface forms are taken from anchor texts
 * - Redirects are resolved
 */

object SaveWikipediaDump2Occs {

    private val LOG = LogFactory.getLog(this.getClass)

    def main(args : Array[String]) {
        val targetFileName = args(0)
        val wikiDumpFileName = args(1)
        val conceptURIsFileName = args(2)
        val redirectTCFileName = args(3)


        LOG.info("Loading concept URIs from "+conceptURIsFileName+"...")
        val conceptURIsSet = Source.fromFile(conceptURIsFileName, "UTF-8").getLines.toSet

        LOG.info("Loading redirects transitive closure from "+redirectTCFileName+"...")
        val redirectsTCMap = Source.fromFile(redirectTCFileName, "UTF-8").getLines.map{ line =>
            val elements = line.split("\t")
            (elements(0), elements(1))
        }.toMap

        val narrowContext = new ContextExtractor(0, 200)

        val filter = new OccurrenceFilter(redirectsTC = redirectsTCMap, conceptURIs = conceptURIsSet, contextExtractor = narrowContext)

        val occs = filter.filter(AllOccurrenceSource.fromXMLDumpFile(new File(wikiDumpFileName)))

        FileOccurrenceSource.writeToFile(occs, new File(targetFileName))

    }
}