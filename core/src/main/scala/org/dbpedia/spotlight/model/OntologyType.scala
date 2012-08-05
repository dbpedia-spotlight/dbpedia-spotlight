/*
 * Copyright 2011 Pablo Mendes, Max Jakob
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

package org.dbpedia.spotlight.model

import java.lang.String

/**
 * Representation of types (DBpedia, Freebase, Schema.org, etc.)
 *
 * @author maxjakob
 * @author Joachim Daiber
 * @author pablomendes (introduced and fixed bug for OntologyType.equals :)
 */

trait OntologyType  {
  def getFullUri : String
  def typeID : String = "OntologyTypeUnknown"

  override def hashCode() : Int = {
    typeID.hashCode()
  }

  override def equals(other : Any) : Boolean = {
      if (other==null)
           false
      else
        other match {
            case o: OntologyType => o.typeID != null && o.typeID.equals(typeID)
            case _ => false;
        }
  }

  override def toString = typeID
}


/**
 * Types from the DBpedia ontology (hierarchical)
 */

class DBpediaType(var name : String) extends OntologyType {

    name = name.replace(DBpediaType.DBPEDIA_ONTOLOGY_PREFIX, "")

    name = name.replace("DBpedia:","")

    name = name.capitalize

    name = name.replaceAll(" ([a-zA-Z])", "$1".toUpperCase).trim

    def equals(that : DBpediaType) : Boolean = {
        name.equalsIgnoreCase(that.name)
    }

    override def getFullUri = DBpediaType.DBPEDIA_ONTOLOGY_PREFIX + name
    override def typeID = new StringBuilder("DBpedia:").append(name).toString()

}

object DBpediaType {
    val DBPEDIA_ONTOLOGY_PREFIX = SpotlightConfiguration.DEFAULT_ONTOLOGY_PREFIX
    val UNKNOWN = new DBpediaType("unknown")
}


/**
 * Types from Freebase: non-hierarchical, grouped into domains.
 */

class FreebaseType(val domain: String, val typeName: String) extends OntologyType {

  override def getFullUri = FreebaseType.FREEBASE_RDF_PREFIX + domain + "." + typeName
  override def typeID = {
    var typeID = "Freebase:/" + domain

    if(typeName != null) {
      typeID += "/" + typeName
    }

    typeID
  }
}

object FreebaseType {

  def fromTypeString(typeString: String) : FreebaseType = {
    val typeParts: Array[String] = typeString.replace(FREEBASE_RDF_PREFIX, "").split("/")

    var domain: String = null
    var theType: String = null
    typeParts.length match {
      case 0 =>
      case 1 => domain = typeParts(0)
      case 2 => domain = typeParts(1)
      case _ => {domain = typeParts(1); theType = typeParts(2)}
    }

    new FreebaseType(domain, theType)
  }

  val FREEBASE_RDF_PREFIX = "http://rdf.freebase.com/ns"
}

class SchemaOrgType(var name : String) extends OntologyType {

    name = name.replace(SchemaOrgType.SCHEMAORG_PREFIX, "")

    def equals(that : SchemaOrgType) : Boolean = {
        name.equalsIgnoreCase(that.name)
    }

    override def getFullUri = SchemaOrgType.SCHEMAORG_PREFIX + name
    override def typeID = "Schema:" + name

   // override def toString = "%s/%s".format(SchemaOrgType.SCHEMAORG_PREFIX,name)

}

object SchemaOrgType {
    val SCHEMAORG_PREFIX = "http://schema.org/"
}