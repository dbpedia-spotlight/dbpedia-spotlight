package org.dbpedia.spotlight.annotate

import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import java.io.File
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.disambiguate.{Disambiguator, DefaultDisambiguator}
import org.dbpedia.spotlight.exceptions.InputException

/**
 * Annotates a text with DBpedia Resources
 */

class DefaultAnnotator(val spotterFile : File, val indexDir : File) extends Annotator {

    private val LOG = LogFactory.getLog(this.getClass)

    //TODO print nice messages for false input!

    LOG.info("Initializing annotator object ...")

    val disambiguator : Disambiguator = new DefaultDisambiguator(indexDir)

    // -- Spotter --
    //val spotter : Spotter = new TrieSpotter(spotterFile)
    val spotter : Spotter = new LingPipeSpotter(spotterFile)

    LOG.info("Done.")

    @throws(classOf[InputException])
    def annotate(text : String) : java.util.List[DBpediaResourceOccurrence] = {
        LOG.info("Spotting...")
        val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = spotter.extract(new Text(text))

        LOG.info("Selecting candidates...");
        val selectedSpots = disambiguator.spotProbability(spottedSurfaceForms);

        LOG.info("Disambiguating... ("+disambiguator.name+")")
        val disambiguatedOccurrences : java.util.List[DBpediaResourceOccurrence] = disambiguator.disambiguate(selectedSpots)

        LOG.info("Done.")
        disambiguatedOccurrences
    }

}