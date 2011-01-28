package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.util.OccurrenceFilter
import io.Source
import java.io.File
import org.dbpedia.spotlight.io.{AllOccurrenceSource, FileOccurrenceSource, DisambiguationContextSource, WikiOccurrenceSource}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 19.09.2010
 * Time: 00:08:48
 * To change this template use File | Settings | File Templates.
 */

object SaveDump2Occs
{
    def main(args : Array[String]) {
        val wikiDumpFileName = "c:/wikipediaDump/en/20100312/enwiki-20100312-pages-articles.xml"
        val preferredURIsFileName = "e:/dbpa/data/preferredURIs.tsv"
        val surfaceFormsFileName = "e:/dbpa/data/surface_forms/surface_forms-Wikipedia-TitRedDis.uniq.sfSorted.tsv"
        val targetFileName = "e:/dbpa/data/Wikipedia_enwiki-20100312-OccDefDis.preferredURIs.restrictSFs.tsv"
        val lowerCaseSurfaceForms = true

        val preferredURIsMap = Source.fromFile(preferredURIsFileName, "UTF-8").getLines.map{ line =>
            val elements = line.split("\t")
            (elements(0), elements(1))
        }.toMap

        var surfaceForms = Map[String,List[String]]()
        Source.fromFile(surfaceFormsFileName, "UTF-8").getLines.foreach{ line =>
            val elements = line.split("\t")
            val sf = elements(0)
            val uri = elements(1)
            surfaceForms = surfaceForms.updated(uri, surfaceForms.get(uri).getOrElse(List[String]()) ::: List(sf))
        }

        // no length limits for context strings because occurrences, definitions and dismabiguation sentences are mixed
        val filter = new OccurrenceFilter(preferredURIsMap = preferredURIsMap,
                                          surfaceForms = surfaceForms,
                                          lowerCaseSurfaceForms = lowerCaseSurfaceForms)

        val occs = filter.filter(AllOccurrenceSource.fromXMLDumpFile(new File(wikiDumpFileName)))

        FileOccurrenceSource.addToFile(occs, targetFileName)

    }
}