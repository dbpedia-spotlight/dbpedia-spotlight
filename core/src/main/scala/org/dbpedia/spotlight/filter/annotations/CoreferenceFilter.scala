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

package org.dbpedia.spotlight.filter.annotations

import org.dbpedia.spotlight.model.{SurfaceForm, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.log.SpotlightLog

/**
 * this is a heuristic and has nothing to do with proper coreference resolution!!!
 * TODO how will this behave when filter is applied on occurrences from different texts? (PABLO)
 * @author maxjakob
 */

class CoreferenceFilter extends AnnotationFilter {

    override def filterOccs(occs : Traversable[DBpediaResourceOccurrence]) : Traversable[DBpediaResourceOccurrence] = {
        // this is a heuristic and has nothing to do with proper coreference resolution!!!
        val occList = occs.toList
        var backwardIdx = occList.length
        occList.reverse.map(laterOcc => {
            backwardIdx -= 1
            val coreferentOcc = occs.slice(0, backwardIdx).find(prevOcc => {
                val coreferring = isCoreferent(prevOcc.surfaceForm, laterOcc.surfaceForm)
                if (coreferring)
                    SpotlightLog.info(this.getClass, "found coreferent: %s at position %d probably coreferring to %s at position %d; copying %s", laterOcc.surfaceForm, laterOcc.textOffset, prevOcc.surfaceForm, prevOcc.textOffset, prevOcc.resource)
                coreferring
            })
            if (coreferentOcc != None) {
                new DBpediaResourceOccurrence(laterOcc.id,
                    coreferentOcc.get.resource,
                    laterOcc.surfaceForm,
                    laterOcc.context,
                    laterOcc.textOffset,
                    laterOcc.provenance,
                    coreferentOcc.get.similarityScore,           // what to put here?
                    coreferentOcc.get.percentageOfSecondRank)    // what to put here?
            }
            else {
                laterOcc
            }
        }).reverse

        //        occs.reverse.filterNot(laterOcc => {
        //            val laterSFWords = laterOcc.surfaceForm.name.split(" ")
        //            backwardIdx -= 1
        //            occs.slice(0, backwardIdx).find(prevOcc => {
        //                val prevSFWords = prevOcc.surfaceForm.name.split(" ")
        //                val isCoreferent = ( (laterSFWords.length == 1 && prevSFWords.contains(laterSFWords.head)) ||
        //                                     (prevSFWords.last equals laterSFWords.last) )
        //                if (isCoreferent)
        //                    SpotlightLog.info(this.getClass, "filtered out as coreferent: %s at position %d probably coreferring to %s at position %d", laterOcc.surfaceForm, laterOcc.textOffset, prevOcc.surfaceForm, prevOcc.textOffset)
        //                isCoreferent
        //            }) != None
        //        }).reverse
    }

    private def isCoreferent(previous : SurfaceForm, later : SurfaceForm) : Boolean = {
        val prevSFWords = previous.name.split(" ")
        val laterSFWords = later.name.split(" ")
        ( (laterSFWords.length == 1 &&
                prevSFWords.filterNot(word => word.substring(0,1) equals word.substring(0,1).toUpperCase).isEmpty &&
                prevSFWords.contains(laterSFWords.head))
                //|| (prevSFWords.last equals laterSFWords.last)
                )
      }

      def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
          // this filter has to operate on a complete set of occurrences in order to find coreferents
          throw new NoSuchMethodError("don't use touchOcc method for this filter; only use filterOccs method")
      }

  }