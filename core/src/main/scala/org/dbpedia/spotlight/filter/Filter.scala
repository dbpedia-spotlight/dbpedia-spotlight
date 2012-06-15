/*
 * Copyright 2011 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.filter

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

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import scala.collection.JavaConversions._

/**
 * Base interface for annotation and occurrence filters.
 * OccurrenceFilters are applied during indexing and AnnotationFilters are applied after disambiguation.
 * @author maxjakob
 */
trait Filter {

    //TODO move to DocumentAnnotationFilter, since it looks at all occurrences at once
    def filterOccs(occs : Traversable[DBpediaResourceOccurrence]) : Traversable[DBpediaResourceOccurrence] = {
        new FilteredOccs(occs)
    }

    def filterOccs(occs : java.util.List[DBpediaResourceOccurrence]) : java.util.List[DBpediaResourceOccurrence] = {
        new FilteredOccs(occs.toList).toList
    }

    private class FilteredOccs(occs : Traversable[DBpediaResourceOccurrence]) extends Traversable[DBpediaResourceOccurrence] {
        override def foreach[U](f : DBpediaResourceOccurrence => U) {
            for(occ <- occs) {
                touchOcc(occ) match {
                    case Some(filteredOcc) => f( filteredOcc )
                    case _ =>
                }
            }
        }
    }

    //TODO move to OccurrenceAnnotationFilter, since it looks at each occurrence at a time
    def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence]

}
