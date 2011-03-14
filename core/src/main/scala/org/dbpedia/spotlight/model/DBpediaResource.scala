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

import scala.collection.JavaConversions._
import org.dbpedia.spotlight.string.ModifiedWikiUtil


class DBpediaResource(var uri : String, var support : Int = 0, var types : List[DBpediaType] = List[DBpediaType]())
{
    require(uri != null)

    uri = uri.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")

    uri = if (ModifiedWikiUtil.isEncoded(uri)) {
              ModifiedWikiUtil.spaceToUnderscore(uri).capitalize
          }
          else {
              ModifiedWikiUtil.wikiEncode(uri)
          }


    def this(uri : String) = {
        this(uri, 0, List[DBpediaType]())
    }

    def this(uri : String, support : Int) = {
        this(uri, support, List[DBpediaType]())
    }

    def equals(that : DBpediaResource) : Boolean = {
        this.uri.equals(that.uri)
    }

    override def hashCode() = {
        this.uri.hashCode
    }

    def setSupport(s : Int) {
        support = s
    }

    def setTypes(typesList : java.util.List[DBpediaType]) {
        types = typesList.toList
    }

    def getTypes : java.util.List[DBpediaType] = types

    override def toString = {
        val typesString = if (types.nonEmpty) types.map(_.name).mkString("(", ",", ")") else ""
        "DBpediaResource["+uri+typesString+"]"
    }

    def getFullUri = DBpediaResource.DBPEDIA_RESOURCE_PREFIX + uri

}

object DBpediaResource {
    val DBPEDIA_RESOURCE_PREFIX = "http://dbpedia.org/resource/"
}