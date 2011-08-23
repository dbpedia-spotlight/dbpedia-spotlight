package org.dbpedia.spotlight.model

import collection.JavaConversions._
import collection.mutable.HashMap

import java.sql.{ResultSet, PreparedStatement, DriverManager}

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

  override def from(dbpediaID : String) : DBpediaResource = {
    val dbpediaResource = new DBpediaResource(dbpediaID)

    val statement: PreparedStatement = sqlConnection.prepareStatement("select * from DBpediaResource WHERE URI=? limit 1;")
    statement.setString(1, dbpediaID)
    val result: ResultSet = statement.executeQuery()
    result.next()

    dbpediaResource.setSupport(result.getInt("COUNT"))
    val dbpType : OntologyType = typeFromID(result.getString("TYPE_DBP").charAt(0))

    var allTypes : List[OntologyType] = result.getString("TYPES_FB").split(0.toChar).toList
      .map(b => typeFromID(b.charAt(0)))

    allTypes = allTypes ::: List[OntologyType](dbpType)

    dbpediaResource.setTypes(allTypes)

    dbpediaResource
  }

  //Create OntologyTypes from the types stored in the database:
  val typeIDMap = new HashMap[Int, OntologyType]()
  var query: ResultSet = sqlConnection.createStatement().executeQuery("select * from OntologyType;")
  while(query.next()) {
    typeIDMap.put(query.getString("TYPE_ID").toList(0).toInt, Factory.OntologyType.from(query.getString("TYPE")))
  }

  //Get the type corresponding to the typeID:
  def typeFromID(typeID : Int) : OntologyType = {
    typeIDMap.get(typeID) match {
      case None => null
      case Some(x) => x
    }
  }


}