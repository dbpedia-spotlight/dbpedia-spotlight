package org.dbpedia.spotlight.filter

import io.Source
import collection.mutable.HashMap
import org.dbpedia.spotlight.model.{OntologyType, FreebaseType, DBpediaType}
import java.sql.{PreparedStatement, Statement, DriverManager}

/**
 * @author Joachim Daiber
 *
 * Importer for information used in DBpediaResource:
 * - count
 * - DBpedia type
 * - Freebase types
 */

object ImporterDBpediaResource {

  def main(args: Array[String]) {
    Class.forName("org.hsqldb.jdbcDriver").newInstance
    val sqlConnection = DriverManager.getConnection(
      "jdbc:hsqldb:file:/Users/jodaiber/Desktop/spotlight-db",
      "sa",
      "")

    val statement : Statement = sqlConnection.createStatement

    //Create Table
    statement.execute("set scriptformat BINARY;"+
      "create table DBpediaResource ( " +
      "\"URI\" Varchar(50) primary key, " +
      "\"COUNT\" Int," +
      "\"TYPE_DBP\" Varchar(1)," +
      "\"TYPES_FB\" Varchar(10), " +
      "UNIQUE (URI) );\n"
    )

    statement.execute("create table OntologyType ( " +
      "\"TYPE_ID\" Varchar(1) primary key, " +
      "\"TYPE\" Varchar(50)," +
      "UNIQUE (TYPE_ID) );\n"
    )


    //Insert all DBpedia types:
    var currentURI : String = ""
    var currentType : String = ""

    var preparedStatement: PreparedStatement = sqlConnection.prepareStatement("insert into DBpediaResource (URI, TYPE_DBP) VALUES (?, ?);")
    for (line <- Source.fromFile("/Users/jodaiber/Desktop/types.dbpedia.tsv", "UTF-8").getLines()) {

      val Array(uri, dbptype) = line.split("\t")
      if(currentURI != uri) {
        //print (typeID(new DBpediaType(currentType)))
        preparedStatement.setString(1, currentURI)
        preparedStatement.setString(2, typeID(new DBpediaType(currentType)).toString)
        preparedStatement.addBatch()

        currentURI = uri
      }
      currentType = dbptype;
    }
    preparedStatement.executeBatch()


    //Update counts:
    preparedStatement = sqlConnection.prepareStatement("update DBpediaResource set \"COUNT\" = ? where URI = ?;")
    for (line <- Source.fromFile("/Users/jodaiber/Desktop/uri.count.tsv", "UTF-8").getLines()) {
      val Array(uri, count) = line.split("\t")
      preparedStatement.setInt(1, count.toInt)
      preparedStatement.setString(2, uri)
      preparedStatement.addBatch()
    }
    preparedStatement.executeBatch()

    
    //Update freebase types:
    preparedStatement = sqlConnection.prepareStatement("update DBpediaResource set \"TYPES_FB\" = ? where URI = ?;")
    for (line <- Source.fromFile("/Users/jodaiber/Desktop/types.freebase.tsv", "UTF-8").getLines()) {
      val Array(uri, fbtypes) = line.split("\t")
      val fbtypeIDs = fbtypes.split(",").map(x => new FreebaseType(x)).map(typeID).mkString(0.toChar.toString)

      preparedStatement.setString(1, fbtypeIDs)
      preparedStatement.setString(2, uri)
      preparedStatement.addBatch()
    }
    preparedStatement.executeBatch()


    //Fill TypeID table
    preparedStatement = sqlConnection.prepareStatement("insert into OntologyType (TYPE_ID, TYPE) VALUES (?, ?);")
    typeIDMap.keys.foreach( typeID => {
      preparedStatement.setString(1, typeIDMap.get(typeID).get.toChar.toString)
      preparedStatement.setString(2, typeID.asInstanceOf[OntologyType].typeID)
      preparedStatement.addBatch()
    })
    preparedStatement.executeBatch()
    
  }

  
  val typeIDMap = new HashMap[OntologyType, Int]()
  def typeID(theType : OntologyType) = {

    if (!typeIDMap.contains(theType)) {
      //We begin with an offset of 1, since 0 is the separator
      typeIDMap.put(theType, typeIDMap.size + 500)

    }

    typeIDMap.get(theType).get.toChar
  }

}