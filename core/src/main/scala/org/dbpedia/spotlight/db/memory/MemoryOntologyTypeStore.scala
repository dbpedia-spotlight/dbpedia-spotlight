package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.OntologyTypeStore
import java.util.HashMap
import org.dbpedia.spotlight.model.{Factory, OntologyType}
import java.lang.{Short, String}

/**
 * @author Joachim Daiber
 *
 */

class MemoryOntologyTypeStore
  extends MemoryStore
  with OntologyTypeStore {

  private val serialVersionUID = 101010109

  var idFromName: HashMap[String, Short] = null
  var ontologyTypeFromID: HashMap[Short, OntologyType] = null

  def size = idFromName.size

  def getOntologyType(id: Short): OntologyType = {
    ontologyTypeFromID.get(id)
  }

  def getOntologyTypeByName(name: String): OntologyType = {
    getOntologyType(idFromName.get(Factory.OntologyType.fromQName(name).typeID))
  }

  def getOntologyTypeByURL(url: String): OntologyType = {
    getOntologyType(idFromName.get(Factory.OntologyType.fromURI(url).typeID))
  }



}
