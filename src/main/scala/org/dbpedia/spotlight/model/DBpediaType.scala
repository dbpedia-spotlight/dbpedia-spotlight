package org.dbpedia.spotlight.model

/**
 * User: Max
 * Date: 30.08.2010
 * Time: 11:04:05
 * Represents a DBpedia type.
 */

class DBpediaType(var name : String) {

    name = name.replace("http://dbpedia.org/ontology/", "")

    def equals(that : DBpediaType) : Boolean = {
        name.equalsIgnoreCase(that.name)
    }

    override def toString = name

}
