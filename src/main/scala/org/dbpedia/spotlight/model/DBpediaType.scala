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

    def equals(that : DBpediaType) : Boolean = {
        name.equalsIgnoreCase(that.name)
    }

    def getFullUri = DBpediaType.DBPEDIA_ONTOLOGY_PREFIX + name

    override def toString = name

}

object DBpediaType {
    val DBPEDIA_ONTOLOGY_PREFIX = "http://dbpedia.org/ontology/"
}
