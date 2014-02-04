package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.model.DBpediaResource
import java.lang.String
import scala.collection.JavaConversions._
import scala.{throws, transient}
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException
import org.dbpedia.spotlight.db.model.{OntologyTypeStore, ResourceStore}
import java.lang.Integer
import util.StringToIDMapFactory

/**
 * @author Joachim Daiber
 */

@SerialVersionUID(1003001)
class MemoryResourceStore
  extends MemoryStore
  with ResourceStore {

  var ontologyTypeStore: OntologyTypeStore = null

  var supportForID: Array[Short] = null
  var uriForID: Array[String] = null
  var typesForID: Array[Array[java.lang.Short]] = null

  @transient
  var idFromURI: java.util.Map[String, Integer] = null

  @transient
  var totalSupport = 0.0

  override def loaded() {
    createReverseLookup()
    SpotlightLog.info(this.getClass, "Counting total support...")
    totalSupport = supportForID.map(q => qc(q)).sum.toDouble
    SpotlightLog.info(this.getClass, "Done.")
  }

  def size = uriForID.size

  def createReverseLookup() {
    if (uriForID != null) {
      SpotlightLog.info(this.getClass, "Creating reverse-lookup for DBpedia resources.")
      idFromURI = StringToIDMapFactory.createDefault(uriForID.size)

      var i = 0
      uriForID foreach { uri => {
        idFromURI.put(uri, i)
        i += 1
      }
      }
    }
  }

  @throws(classOf[DBpediaResourceNotFoundException])
  def getResource(id: Int): DBpediaResource = {

    val uri = try {
      uriForID(id)
    } catch {
      case e: java.lang.ArrayIndexOutOfBoundsException => null
    }

    if (uri == null)
      throw new DBpediaResourceNotFoundException("DBpediaResource %s not found.".format(uri))

    val support = supportForID(id)
    val typeIDs = typesForID(id)

    val res = new DBpediaResource(uri, qc(support))
    res.uri = uri

    res.id = id
    res.setTypes((typeIDs map { typeID: java.lang.Short => ontologyTypeStore.getOntologyType(typeID) }).toList)

    res.setPrior(res.support / totalSupport)

    res
  }

  @throws(classOf[DBpediaResourceNotFoundException])
  def getResourceByName(name: String): DBpediaResource = {
    idFromURI.get(name) match {
      case id: Integer if id > 0 => getResource(id)
      case _ => throw new DBpediaResourceNotFoundException("Could not find %s".format(name))
    }
  }



}
