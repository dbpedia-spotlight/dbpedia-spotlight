package org.dbpedia.spotlight.annotate

import java.io.File
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.disambiguate.{Disambiguator, DefaultDisambiguator}
import org.dbpedia.spotlight.exceptions.InputException
import org.dbpedia.spotlight.spot.lingpipe.{IndexLingPipeSpotter, LingPipeSpotter}

/**
 * Annotates a text with DBpedia Resources
 */

class DefaultAnnotator(val spotter : Spotter, val disambiguator: Disambiguator) extends Annotator {

    def this(spotterFile: File, indexDir : File) {
        this(new LingPipeSpotter(spotterFile), new DefaultDisambiguator(indexDir))
    }

    def this(spotter : Spotter, indexDir : File) {
        this(spotter, new DefaultDisambiguator(indexDir))
    }

    def this(spotterFile: File, disambiguator: Disambiguator) {
        this(new LingPipeSpotter(spotterFile), disambiguator)
    }

    private val LOG = LogFactory.getLog(this.getClass)


    @throws(classOf[InputException])
    def annotate(text : String) : java.util.List[DBpediaResourceOccurrence] = {
        LOG.info("Spotting...")
        val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = spotter.extract(new Text(text))

        //LOG.info("Selecting candidates...");
        //val selectedSpots = disambiguator.spotProbability(spottedSurfaceForms);
        LOG.info("Skipping candidate selection.");
        val selectedSpots = spottedSurfaceForms

        LOG.info("Disambiguating... ("+disambiguator.name+")")
        val disambiguatedOccurrences : java.util.List[DBpediaResourceOccurrence] = disambiguator.disambiguate(selectedSpots)

        LOG.info("Done.")
        disambiguatedOccurrences
    }

}