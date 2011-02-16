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

package org.dbpedia.spotlight.io

import java.io.File
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{DBpediaType, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.util.TypesLoader

/**
 * User: Max
 * Date: 27.08.2010
 * Time: 14:44:52
 * Adds types to Occurrences
 */

class TypeAdder(val occSource : OccurrenceSource, var typesMap : Map[String,List[DBpediaType]]) extends OccurrenceSource
{
    private val LOG = LogFactory.getLog(this.getClass)
    
    def this(occSource : OccurrenceSource, typesFile : File) = {
        this(occSource, TypesLoader.getTypesMap(typesFile))
    }

    override def foreach[U](f : DBpediaResourceOccurrence => U) {
        for (occ <- occSource) {
            if (occ.resource.types.isEmpty) {
                occ.resource.types = typesMap.get(occ.resource.uri).getOrElse(List[DBpediaType]())
                f( new DBpediaResourceOccurrence(occ.id,
                                                 occ.resource,
                                                 occ.surfaceForm,
                                                 occ.context,
                                                 occ.textOffset,
                                                 occ.provenance,
                                                 occ.similarityScore) )
            }
            else {
                f( occ )
            }
        }
    }
   
}