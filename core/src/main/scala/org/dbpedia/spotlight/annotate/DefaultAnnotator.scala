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

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.exceptions.InputException
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.disambiguate.{ParagraphDisambiguatorJ, ParagraphDisambiguator, Disambiguator}
import java.util

/**
 * Annotates a text with DBpedia Resources.
 * This is just an example of how to wire the steps in our pipeline with default configurations.
 *
 * @author maxjakob, pablomendes
 */
class DefaultAnnotator(val spotter : Spotter, val disambiguator: Disambiguator) extends Annotator {

    @throws(classOf[InputException])
    def annotate(text : String) : java.util.List[DBpediaResourceOccurrence] = {

        SpotlightLog.info(this.getClass, "Spotting... (%s)", spotter.getName)
        val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = spotter.extract(new Text(text))

        SpotlightLog.info(this.getClass, "Disambiguating... (%s)", disambiguator.name)
        val disambiguatedOccurrences : java.util.List[DBpediaResourceOccurrence] = {
          if(spottedSurfaceForms.length>0)
            disambiguator.disambiguate(spottedSurfaceForms)
          else
            new util.ArrayList[DBpediaResourceOccurrence]()
        }

        SpotlightLog.info(this.getClass, "Done.")
        disambiguatedOccurrences
    }

}

class DefaultParagraphAnnotator(val spotter : Spotter, val disambiguator: ParagraphDisambiguatorJ) extends ParagraphAnnotator {

    @throws(classOf[InputException])
    def annotate(text : String) : java.util.List[DBpediaResourceOccurrence] = {

        SpotlightLog.info(this.getClass, "Spotting... (%s)", spotter.getName)
        val spottedSurfaceForms : List[SurfaceFormOccurrence] = asBuffer(spotter.extract(new Text(text))).toList

        SpotlightLog.info(this.getClass, "Disambiguating... (%s)", disambiguator.name)
        val disambiguatedOccurrences : java.util.List[DBpediaResourceOccurrence] = {
          if(spottedSurfaceForms.length>0)
            disambiguator.disambiguate(Factory.Paragraph.from(spottedSurfaceForms))
          else
            new util.ArrayList[DBpediaResourceOccurrence]()
        }


        SpotlightLog.info(this.getClass, "Done.")
        disambiguatedOccurrences
    }

}