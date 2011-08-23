package org.dbpedia.spotlight.model

/**
 * @author maxjakob
 * @author Joachim Daiber
 *
 * Representation of types (DBpedia, Freebase, Schema.org, etc.)
 *
 */

trait OntologyType {
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
 * Types from freebase: non-hierarchical, grouped into domains.
 */

class FreebaseType(var domain : String, var typeName : String) extends OntologyType {

  def this(typeString : String) {
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