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

package org.dbpedia.spotlight.annotate

import java.io.File
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.disambiguate.{Disambiguator, DefaultDisambiguator}
import org.dbpedia.spotlight.exceptions.InputException
import org.dbpedia.spotlight.spot.lingpipe.{IndexLingPipeSpotter, LingPipeSpotter}
import org.dbpedia.spotlight.candidate.{SpotSelector, CommonWordFilter}

/**
 * Annotates a text with DBpedia Resources
 */

class DefaultAnnotator(val spotter : Spotter, val spotSelector: SpotSelector, val disambiguator: Disambiguator) extends Annotator {

//    def this(spotter : Spotter, disambiguator : Disambiguator) = {
//        this(spotter, null, disambiguator);
//    }
//
//    def this(spotterFile: File, indexDir : File) {
//        this(new LingPipeSpotter(spotterFile), new DefaultDisambiguator(indexDir))
//    }
//
//    def this(spotterFile: File, spotSelectorFile: File, indexDir : File) {
//        this(new LingPipeSpotter(spotterFile), new CommonWordFilter(spotSelectorFile.getPath), new DefaultDisambiguator(indexDir))
//    }
//
//    def this(spotter : Spotter, indexDir : File) {
//        this(spotter, new DefaultDisambiguator(indexDir))
//    }
//
//    def this(spotterFile: File, disambiguator: Disambiguator) {
//        this(new LingPipeSpotter(spotterFile), disambiguator)
//    }

    private val LOG = LogFactory.getLog(this.getClass)

    @throws(classOf[InputException])
    def annotate(text : String) : java.util.List[DBpediaResourceOccurrence] = {
        LOG.info("Spotting...")
        val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = spotter.extract(new Text(text))

        val selectedSpots = if (spotSelector==null) {
            LOG.info("Skipping candidate selection.");
            spottedSurfaceForms
        } else  {
            LOG.info("Selecting candidates...");

            // Old selector that we never quite finished coding
            //val selectedSpots = disambiguator.spotProbability(spottedSurfaceForms);
            val previousSize = spottedSurfaceForms.size
            // New selector
            val r = spotSelector.select(spottedSurfaceForms)
            val count = previousSize-r.size
            val percent = if (count==0) "0" else "%1.0f" format ((count.toDouble / previousSize) * 100)
            LOG.info(String.format("Removed %s (%s percent) spots using spotSelector %s", count.toString, percent.toString, spotSelector.getClass().getSimpleName ))
            r
        };

        LOG.info("Disambiguating... ("+disambiguator.name+")")
        val disambiguatedOccurrences : java.util.List[DBpediaResourceOccurrence] = disambiguator.disambiguate(selectedSpots)

        LOG.info("Done.")
        disambiguatedOccurrences
    }

}