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

import collection.JavaConversions._
import collection.mutable.HashMap

import org.dbpedia.spotlight.lucene.search.BaseSearcher
import org.dbpedia.spotlight.lucene.LuceneManager
import java.sql.{SQLException, ResultSet, PreparedStatement, DriverManager}

import java.lang.String
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, SearchException}
import collection.immutable.List._

/**
 * @author Joachim Daiber
 *
 * Factory for creating {@link DBpediaResource}s from the DBpedia ID String.
 * Implementations may use the Lucene Index or a local SQL database.
 */

trait DBpediaResourceFactory {
    def from(dbpediaID : String): DBpediaResource
}

class DBpediaResourceFactorySQL(sqlDriver : String, sqlConnector : String, username : String, password : String) extends DBpediaResourceFactory {

    //Initialize SQL connection:
    Class.forName(sqlDriver).newInstance()
    val sqlConnection = DriverManager.getConnection(
        sqlConnector,
        username,
        password)

    val totalNumberOfOccs =  69772256; //TODO get this from database. a global table can store TOTAL_OCCURRENCES, or a sum can be obtained at startup

    override def from(dbpediaID : String) : DBpediaResource = {
        val dbpediaResource = new DBpediaResource(dbpediaID)

        val statement: PreparedStatement = sqlConnection.prepareStatement("select * from DBpediaResource WHERE URI=? limit 1;")
        statement.setString(1, dbpediaID)
        val result: ResultSet = statement.executeQuery()

        if (result.next() == false) {
            throw new ItemNotFoundException("Did not find DBpediaResource in database.")
        }
        
        val count = result.getInt("COUNT")

        dbpediaResource.setSupport(count)
        
        val typesString = Option(result.getString("TYPES"))
        var allTypes : List[OntologyType] = (typesFromID(typesString) ::: Nil)
        allTypes :::= allTypes.flatMap(x => typeToSchemaType.get(x))

        dbpediaResource.setTypes(allTypes.filter(t => t != null))

        //TODO smoothing
        dbpediaResource.setPrior( (dbpediaResource.support.toDouble+1) / totalNumberOfOccs.toDouble)

        dbpediaResource
    }

    //Create OntologyTypes from the types stored in the database:
    val typeIDMap = new HashMap[Int, OntologyType]()
    var query: ResultSet = sqlConnection.createStatement().executeQuery("select * from OntologyType;")
    while(query.next()) {
        typeIDMap.put(query.getString("TYPE_ID").toList(0).toInt, Factory.OntologyType.fromQName(query.getString("TYPE")))
    }

    //Read OntologyType <-> Schema.org mapping
    val typeToSchemaType = new HashMap[OntologyType, OntologyType]()
    query = sqlConnection.createStatement().executeQuery("select * from SchemaOrgMapping;")
    while(query.next()) {
        typeToSchemaType.put(Factory.OntologyType.fromQName(query.getString("TYPE_ONTOLOGY")), Factory.OntologyType.fromQName(query.getString("TYPE_SCHEMA")))
    }


    def typesFromID(types: Option[String]) : List[OntologyType] = {
        types match {
            case Some(t) => t.toCharArray.toList.flatMap( id => typeIDMap.get(id) )
            case None => List[OntologyType]()
        }
    }

}

class DBpediaResourceFactoryLucene(val luceneManager: LuceneManager, val searcher: BaseSearcher) {

    @throws(classOf[SearchException])
    def from(dbpediaID : String): DBpediaResource = {
        from(searcher.getHits(luceneManager.getQuery(new DBpediaResource(dbpediaID))).head.doc)
    }

    def from(luceneDocId : Int): DBpediaResource = {
        searcher.getDBpediaResource(luceneDocId)
    }

}
