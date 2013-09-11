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
import org.dbpedia.spotlight.string.ContextExtractor
import org.dbpedia.spotlight.exceptions.InputException
import org.dbpedia.spotlight.log.SpotlightLog


class ContextNarrowFilter(val contextExtractor : ContextExtractor) extends OccurrenceFilter {

    def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
        try {
            Some(contextExtractor.narrowContext(occ))
        }
        catch {
            case e : InputException => {
                SpotlightLog.warn(this.getClass, "filtered out occurrence %s. too little context", occ.id)
                None
            }
        }
    }

}