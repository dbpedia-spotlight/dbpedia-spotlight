package org.dbpedia.spotlight.filter.occurrences

import org.dbpedia.spotlight.model.{SurfaceForm, DBpediaResource, DBpediaResourceOccurrence}

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

class LowerCaseSurfaceFormsFilter extends OccurrenceFilter {

    def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
        val lowerCasedSf = new SurfaceForm(occ.surfaceForm.name) //FIXME what about other properties of SurfaceForm (e.g.spot prob?)
        Some(new DBpediaResourceOccurrence(occ.id, occ.resource, lowerCasedSf, occ.context, occ.textOffset, occ.provenance, occ.similarityScore, occ.percentageOfSecondRank, occ.contextualScore))
    }

}