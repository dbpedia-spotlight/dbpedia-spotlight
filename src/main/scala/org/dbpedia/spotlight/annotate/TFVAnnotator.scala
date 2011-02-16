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

import org.dbpedia.spotlight.model.{SurfaceFormOccurrence, Text, DBpediaResourceOccurrence}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.disambiguate.Disambiguator
import org.dbpedia.spotlight.spot.Spotter

/**
 * Annotates a text with DBpedia Resources
 */

class TFVAnnotator(val spotter : Spotter, val disambiguator : Disambiguator) extends Annotator
{
    private val LOG = LogFactory.getLog(this.getClass)

    def annotate(text : String) : java.util.List[DBpediaResourceOccurrence] =
    {
        // spot surface forms in the string
        val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = spotter.extract(new Text(text))

        // disambiguate surface form occurrences to DBpedia resources
        val resources : java.util.List[DBpediaResourceOccurrence] = disambiguator.disambiguate(spottedSurfaceForms)
        // here was a try / catch block before for SearchException.
        // this should be done one level down, so that only one disambiguation breaks, not for the complete list

        resources
    }


    //def disambiguator() : Disambiguator = { disambiguator }

    //def spotter() : Spotter = { spotter }

//    def explain(occ: DBpediaResourceOccurrence, nExplanations: Int) : java.util.List[Explanation] = {
//        disambiguator.explain(occ, nExplanations)
//    }
    
}