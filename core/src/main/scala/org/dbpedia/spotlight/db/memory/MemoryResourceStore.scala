package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.model.DBpediaResource
import gnu.trove.TObjectIntHashMap
import java.lang.{Short, String}
import scala.collection.JavaConversions._
import scala.{throws, transient}
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, DBpediaResourceNotFoundException}
import org.dbpedia.spotlight.db.model.{OntologyTypeStore, ResourceStore}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

@SerialVersionUID(1003001)
class MemoryResourceStore
  extends MemoryStore
  with ResourceStore {

  var ontologyTypeStore: OntologyTypeStore = null

  var supportForID: Array[Int] = null
  var uriForID: Array[String] = null
  var typesForID: Array[Array[Short]] = null

  @transient
  var idFromURI: TObjectIntHashMap = null

  @transient
  var totalSupport = 0.0

  override def loaded() {
    createReverseLookup()
    LOG.info("Counting total support...")
    totalSupport = supportForID.sum.toDouble
    LOG.info("Done.")
  }

  def size = uriForID.size

  def createReverseLookup() {
    if (uriForID != null) {
      System.err.println("Creating reverse-lookup for DBpedia resources.")
      idFromURI = new TObjectIntHashMap(uriForID.size)

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

    val res = new DBpediaResource(uri, support)
    res.id = id
    res.setTypes((typeIDs map { typeID: Short => ontologyTypeStore.getOntologyType(typeID) }).toList)

    res.setPrior(res.support / totalSupport)

    res
  }

  @throws(classOf[DBpediaResourceNotFoundException])
  def getResourceByName(name: String): DBpediaResource = {
    idFromURI.get(name) match {
      case id: Int if id > 0 => getResource(id)
      case id: Int if id == 0 => throw new DBpediaResourceNotFoundException("Could not find %s".format(name))
    }
  }



}
