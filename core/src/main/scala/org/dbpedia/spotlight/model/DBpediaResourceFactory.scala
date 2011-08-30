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

import java.util.logging.Logger
import org.apache.commons.logging.LogFactory
import java.lang.String
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, SearchException}

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

    private val LOG = LogFactory.getLog(this.getClass)

    //Initialize SQL connection:
    Class.forName(sqlDriver).newInstance()
    val sqlConnection = DriverManager.getConnection(
        sqlConnector,
        username,
        password)

    override def from(dbpediaID : String) : DBpediaResource = {
        val dbpediaResource = new DBpediaResource(dbpediaID)

        val statement: PreparedStatement = sqlConnection.prepareStatement("select * from DBpediaResource WHERE URI=? limit 1;")
        statement.setString(1, dbpediaID)
        val result: ResultSet = statement.executeQuery()

        result.next() match {
            case false => throw new ItemNotFoundException("Did not find DBpediaResource in database.")
        }

        dbpediaResource.setSupport(result.getInt("COUNT"))
        val dbpediaTypeString = result.getString("TYPE_DBP")
        val fbTypes = result.getString("TYPES_FB")

        var allTypes : List[OntologyType] = if(fbTypes == null) {
            List[OntologyType]()
        }else{
            fbTypes.toCharArray.toList
                .map(b => typeFromID(b)).filter(t => t != null)
        }

        if(dbpediaTypeString != null) {
            val dbpType : OntologyType = typeFromID(dbpediaTypeString.charAt(0))
            allTypes = allTypes ::: List[OntologyType](dbpType)
        }

        dbpediaResource.setTypes(allTypes)

        dbpediaResource
    }

    //Create OntologyTypes from the types stored in the database:
    val typeIDMap = new HashMap[Int, OntologyType]()
    var query: ResultSet = sqlConnection.createStatement().executeQuery("select * from OntologyType;")
    while(query.next()) {
        var string: String = query.getString("TYPE")
        typeIDMap.put(query.getString("TYPE_ID").toList(0).toInt, Factory.OntologyType.fromQName(string))
    }

    //Get the type corresponding to the typeID:
    def typeFromID(typeID : Int) : OntologyType = {
        typeIDMap.get(typeID) match {
            case None => null
            case Some(x) => x
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
