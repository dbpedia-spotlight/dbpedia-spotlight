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

import org.dbpedia.spotlight.model.{DBpediaResource, DBpediaResourceOccurrence}

/**
 *
 * TODO i18n can transform this class into a BlacklistedPatternsFilter and read patterns from file.
 */
class ListPagesFilter extends OccurrenceFilter {

    val listPrefix = "List_of_"

    def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
        if(!isListResource(occ.resource) && !isListSource(occ)) {
            Some(occ)
        }
        else {
            None
        }
    }

    def isListResource(resource : DBpediaResource) : Boolean = {
        resource.uri startsWith listPrefix
    }

    def isListSource(occ : DBpediaResourceOccurrence) : Boolean = {
        occ.id startsWith listPrefix
    }

}