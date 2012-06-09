package org.dbpedia.spotlight.filter

import io.Source
import collection.mutable.HashMap
import org.dbpedia.spotlight.model.{Factory, OntologyType, FreebaseType, DBpediaType}
import java.sql.{PreparedStatement, Statement, DriverManager}
import java.lang.String


/**
 * @author Joachim Daiber
 *
 * Importer for information used in DBpediaResource:
 * - count
 * - DBpedia type
 * - Freebase types
 * - Schema.org types
 */

object ImporterDBpediaResource {

  def typesToString(theTypes: Traversable[OntologyType]) : String = (theTypes.foldLeft(new StringBuilder())((sb, theType) => sb + typeID(theType))).toString()

  def main(args: Array[String]) {

    //Concepts and counts:
    val conceptList: String = "/Users/jodaiber/Desktop/DBpedia/conceptURIs.list"
    val uriCounts: String = "/Users/jodaiber/Desktop/DBpedia/uri.count.tsv"

    //Types:
    val typesDBpedia: String = "../../../../../scripts/types.dbpedia.tsv"
    val typesFreebase: String = "../../../../../scripts/types.freebase.tsv"
    val typeMappingSchemaOrg: String = "../../../../../scripts/typemapping.schema_org.tsv"


    Class.forName("org.hsqldb.jdbcDriver").newInstance
    val sqlConnection = DriverManager.getConnection(
      "jdbc:hsqldb:file:/data/spotlight/spotlight-db",
      "sa",
      "")

    val statement: Statement = sqlConnection.createStatement

    //Set storage format (specific to HSQLDB):
    statement.execute("set scriptformat BINARY;")

    //Create Table
    statement.execute(
      "create table DBpediaResourceO ( " +
        "\"URI\" Varchar(100) primary key, " +
        "\"COUNT\" Int," +
        "\"TYPES_DBP\" Varchar(10)," +
        "\"TYPES_FB\" Varchar(50), " +
        "UNIQUE (URI) );\n"
    )

    statement.execute("create table OntologyType ( " +
      "\"TYPE_ID\" Varchar(1) primary key, " +
      "\"TYPE\" Varchar(50)," +
      "UNIQUE (TYPE_ID) );\n"
    )

    statement.execute("create table SchemaOrgMapping ( " +
      "\"TYPE_ONTOLOGY\" Varchar(50)," +
      "\"TYPE_SCHEMA\" Varchar(50)," +
      "UNIQUE (TYPE_ONTOLOGY) );\n"
    )

    System.err.println("Reading concepts...")
    var preparedStatement: PreparedStatement = sqlConnection.prepareStatement("insert into DBpediaResourceO (URI) VALUES (?);")
    for (line <- Source.fromFile(conceptList, "UTF-8").getLines()) {
      try {
        val uri = line
        preparedStatement.setString(1, uri)
        preparedStatement.execute()
      } catch{
        case e: Exception =>
      }
    }

    //Insert all DBpedia types:
    var currentTypes = List[OntologyType]()
    var currentURI : String = ""

    System.err.println("Reading DBpedia types...")
    preparedStatement = sqlConnection.prepareStatement("update DBpediaResourceO set \"TYPES_DBP\" = ? where URI = ?;")
    for (Array(uri, dbptype) <- Source.fromFile(typesDBpedia, "UTF-8").getLines().map(x => x.split("\t"))) {

      if(currentURI != uri) {
        preparedStatement.setString(1, typesToString(currentTypes))
        preparedStatement.setString(2, currentURI)
        preparedStatement.execute()

        currentURI = uri
        currentTypes = List[OntologyType]()
      }

      if (dbptype.startsWith(DBpediaType.DBPEDIA_ONTOLOGY_PREFIX))
        currentTypes :+= new DBpediaType(dbptype)
    }

    System.err.println("Reading Freebase types...")
    preparedStatement = sqlConnection.prepareStatement("update DBpediaResourceO set \"TYPES_FB\" = ? where URI = ?;")

    for (line <- Source.fromFile(typesFreebase, "UTF-8").getLines()) {
      val Array(uri, fbtypes) = line.split("\t")
      val fbtypeIDs : String = typesToString(fbtypes.split(",").map(x => FreebaseType.fromTypeString(x)))

      preparedStatement.setString(1, fbtypeIDs)
      preparedStatement.setString(2, uri)
      preparedStatement.execute()
    }

    System.err.println("Reading counts...")
    preparedStatement = sqlConnection.prepareStatement("update DBpediaResourceO set \"COUNT\" = ? where URI = ?;")
    for (line <- Source.fromFile(uriCounts, "UTF-8").getLines()) {
      val Array(uri, count) = line.split("\t")
      preparedStatement.setInt(1, count.toInt)
      preparedStatement.setString(2, uri)
      preparedStatement.execute()
    }


    System.err.println("Cleaning database...")
    System.gc()
    statement.execute("create table DBpediaResource ( " +
      "\"URI\" Varchar(100) primary key, " +
      "\"COUNT\" Int," +
      "\"TYPES\" Varchar(40)," +
      "UNIQUE (URI) );\n"
    )
    preparedStatement = sqlConnection.prepareStatement("INSERT INTO DBpediaResource " +
      "SELECT DBpediaResourceO.URI, DBpediaResourceO.COUNT, CONCAT(IFNULL(DBpediaResourceO.TYPES_DBP, ?), IFNULL(DBpediaResourceO.TYPES_FB, ?)) FROM DBpediaResourceO;")
    preparedStatement.setString(1, "")
    preparedStatement.setString(2, "")
    preparedStatement.execute()

    statement.execute("DROP TABLE DBpediaResourceO;")


    //Fill TypeID table
    preparedStatement = sqlConnection.prepareStatement("insert into OntologyType (TYPE_ID, TYPE) VALUES (?, ?);")
    typeIDMap.keys.foreach( typeID => {
      preparedStatement.setString(1, typeIDMap.get(typeID).get.toChar.toString)
      preparedStatement.setString(2, typeID.asInstanceOf[OntologyType].typeID)
      preparedStatement.addBatch()
    })
    preparedStatement.executeBatch()

    //Write DBpedia <-> Schema type mapping
    preparedStatement = sqlConnection.prepareStatement("insert into SchemaOrgMapping (TYPE_ONTOLOGY, TYPE_SCHEMA) VALUES (?, ?);")

    for (Array(dbpType, schemaType) <- Source.fromFile(typeMappingSchemaOrg, "UTF-8").getLines().map(x => x.split("\t"))) {
      preparedStatement.setString(1, Factory.OntologyType.fromURI(dbpType).typeID)
      preparedStatement.setString(2, Factory.OntologyType.fromURI(schemaType).typeID)
      preparedStatement.execute()
    }


    //Set database to read only mode:
    statement.execute("SET READONLY TRUE;")

    //Shut down database => all logged queries are written to the database.
    statement.execute("SHUTDOWN;")

  }

  val typeIDMap: HashMap[OntologyType, Char] = new HashMap[OntologyType, Char]()
  def typeID(theType : OntologyType) : Char = typeIDMap.getOrElseUpdate(theType, typeIDMap.size.asInstanceOf[Char])

}