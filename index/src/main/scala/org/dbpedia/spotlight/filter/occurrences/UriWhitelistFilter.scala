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

package org.dbpedia.spotlight.filter.occurrences

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import io.Source
import org.dbpedia.spotlight.log.SpotlightLog
import java.io.File

/**
 * Class that takes a whitelist of URIs to allow for indexing.
 * Used during indexing to eliminate redirects and disambiguations, keeping only URIs that denote entities/concepts.
 *
 * @author maxjakob
 */
class UriWhitelistFilter(val whitelistedUris : Set[String]) extends OccurrenceFilter {

    def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
        if(whitelistedUris contains occ.resource.uri) {
            Some(occ)
        }
        else {
            None
        }
    }

}

object UriWhitelistFilter {
    def fromFile(conceptURIsFileName: File) = {
        SpotlightLog.info(this.getClass, "Loading concept URIs from %s...", conceptURIsFileName)
        val conceptUrisSet = Source.fromFile(conceptURIsFileName, "UTF-8").getLines.toSet
        new UriWhitelistFilter(conceptUrisSet)
    }
}