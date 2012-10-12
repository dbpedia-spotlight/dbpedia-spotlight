package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.OntologyType
import java.lang.{Short, String}

/**
 * @author Joachim Daiber
 */

trait OntologyTypeStore {

  def getOntologyType(id: Short): OntologyType
  def getOntologyTypeByName(name: String): OntologyType
  def getOntologyTypeByURL(url: String): OntologyType

}
