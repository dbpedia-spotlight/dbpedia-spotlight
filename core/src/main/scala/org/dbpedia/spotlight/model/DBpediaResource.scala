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
import org.dbpedia.extraction.util.WikiUtil


@serializable
class DBpediaResource(var uri : String,
                      var support : Int = 0,
                      var prior : Double = 0.0,
                      var types : List[OntologyType] = List[OntologyType]())
{
    var id: Int = 0

    require(uri != null)

    uri = uri.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "")

    uri = if (isEncoded(uri)) uri else WikiUtil.wikiEncode(uri)

    def this(uri : String) = {
        this(uri, 0, 0.0, List[OntologyType]())
    }

    def this(uri : String, support : Int) = {
        this(uri, support, 0.0, List[OntologyType]())
    }

    def this(uri : String, support : Int, prior : Double) = {
        this(uri, support, prior, List[OntologyType]())
    }

    override def equals(obj : Any) : Boolean = {
        obj match {
            case that: DBpediaResource => this.uri.equals(that.uri)
            case _ => obj.equals(this)
        }
    }

    override def hashCode() = {
        this.uri.hashCode
    }

    def setSupport(s : Int) {
        support = s
    }

    def setPrior(s : Double) {
        prior = s
    }

    def setTypes(typesList : java.util.List[_<:OntologyType]) {
        types = typesList.toList
    }

    def getTypes : java.util.List[OntologyType] = types

    override def toString = {
        val typesString = if (types!=null && types.nonEmpty) types.filter(_!=null).filter(_.typeID!=null).map(_.typeID).mkString("(", ",", ")") else ""

        if (isExternalURI) {
            "WiktionaryResource["+uri+typesString+"]"
        } else {
            "DBpediaResource["+uri+typesString+"]"
        }
    }

    /**
     * This means that it is not from our default namespace
     */
    def isExternalURI = {
        uri.startsWith("http://")
    }

    def getFullUri = {
        if (isExternalURI) {
            uri
        } else {
            SpotlightConfiguration.DEFAULT_NAMESPACE + uri
        }
    }

  // heuristic!!
  private def isEncoded(s : String) = """%[0-9a-fA-F][0-9a-fA-F]""".r.findFirstIn(s) != None

}
