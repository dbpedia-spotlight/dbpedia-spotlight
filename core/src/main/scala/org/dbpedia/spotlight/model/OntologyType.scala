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

/**
 * Representation of types (DBpedia, Freebase, Schema.org, etc.)
 *
 * @author maxjakob
 * @author Joachim Daiber
 */

trait OntologyType  {
  def getFullUri : String
  def typeID : String

  override def hashCode() : Int = {
    typeID.hashCode()
  }

  override def equals(other : Any) : Boolean = {
    other.isInstanceOf[OntologyType] &&
    other.asInstanceOf[OntologyType].typeID.equals(typeID)
  }
  
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
    override def typeID = "DBpedia:" + name

    override def toString = name

}

object DBpediaType {
    val DBPEDIA_ONTOLOGY_PREFIX = "http://dbpedia.org/ontology/"
    val UNKNOWN = new DBpediaType("unknown")
}


/**
 * Types from Freebase: non-hierarchical, grouped into domains.
 */

class FreebaseType(var domain : String, var typeName : String) extends OntologyType {

  def this(typeString : String) { //TODO how to instantiate a FreebaseType from a full URI? http://rdf.freebase.com/ns/common/topic
    this(typeString.split("/")(1), typeString.split("/")(2))
  }
  
  override def getFullUri = "http://rdf.freebase.com/ns/" + domain + "." + typeName
  override def typeID = "Freebase:/" + domain + "/" + typeName
  override def toString = FreebaseType.FREEBASE_RDF_PREFIX + domain + "/" + typeName
}

object FreebaseType {
    val FREEBASE_RDF_PREFIX = "http://rdf.freebase.com/ns/"
    val UNKNOWN = new FreebaseType("common", "topic")
}

object SchemaOrgType {
    val SCHEMAORG_PREFIX = "http://schema.org/"
    val UNKNOWN = new SchemaOrgType("Thing")
}

class SchemaOrgType(var name : String) extends OntologyType {

    name = name.replace(SchemaOrgType.SCHEMAORG_PREFIX, "")

    def equals(that : SchemaOrgType) : Boolean = {
        name.equalsIgnoreCase(that.name)
    }

    override def getFullUri = SchemaOrgType.SCHEMAORG_PREFIX + name
    override def typeID = "Schema:" + name

    override def toString = "%s/%s".format(SchemaOrgType.SCHEMAORG_PREFIX,name)

}