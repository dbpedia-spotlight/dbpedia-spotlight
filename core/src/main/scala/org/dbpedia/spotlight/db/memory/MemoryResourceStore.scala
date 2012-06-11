package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.model.DBpediaResource
import org.dbpedia.spotlight.db.model.ResourceStore
import gnu.trove.TObjectIntHashMap
import java.lang.{Short, String}
import scala.collection.JavaConversions._
import scala.{throws, transient}
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, DBpediaResourceNotFoundException}

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

  var ontologyTypeStore: MemoryOntologyTypeStore = null

  var supportForID: Array[Int] = null
  var uriForID: Array[String] = null
  var typesForID: Array[Array[Short]] = null

  @transient
  var idFromURI: TObjectIntHashMap = null

  override def loaded() {
    createReverseLookup()
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

    val uri     = uriForID(id)

    if (uri == null)
      throw new SurfaceFormNotFoundException("DBpediaResource %s not found.".format(uri))

    val support = supportForID(id)
    val typeIDs = typesForID(id)

    val res = new DBpediaResource(uri, support)
    res.id = id
    res.setTypes((typeIDs map { typeID: Short => ontologyTypeStore.getOntologyType(typeID) }).toList)

    res
  }

  @throws(classOf[DBpediaResourceNotFoundException])
  def getResourceByName(name: String): DBpediaResource = {
    getResource(idFromURI.get(name))
  }



}
