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

package org.dbpedia.spotlight.model

/**
 * User: Max
 * Date: 30.08.2010
 * Time: 11:04:05
 * Represents a DBpedia type.
 */

class DBpediaType(var name : String) {

    name = name.replace(DBpediaType.DBPEDIA_ONTOLOGY_PREFIX, "")

    name = name.capitalize

    name = name.replaceAll(" ([a-zA-Z])", "$1".toUpperCase).trim

    def equals(that : DBpediaType) : Boolean = {
        name.equalsIgnoreCase(that.name)
    }

    def getFullUri = DBpediaType.DBPEDIA_ONTOLOGY_PREFIX + name

    override def toString = name

}

object DBpediaType {
    val DBPEDIA_ONTOLOGY_PREFIX = "http://dbpedia.org/ontology/"
    val UNKNOWN = new DBpediaType("unknown")
}
