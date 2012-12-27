package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.OntologyType
import java.lang.{Short, String}

/**
 *  A store interface for ontology types. Ontology types are currently
 *  - DBpedia Ontology
 *  - Schema.org
 *  - Freebase
 *
 * @author Joachim Daiber
 */

trait OntologyTypeStore {

  /**
   * Returns the OntologyType for the internal id (used for efficient storage).
   *
   * @param id id of the ontology type.
   * @return
   */
  def getOntologyType(id: Short): OntologyType

  /**
   * Get the ontology type for its short name.
   *
   * @see Factory.OntologyType.fromQName
   *
   * @param name short name for an ontology type
   * @return
   */
  def getOntologyTypeByName(name: String): OntologyType


  /**
   * Returns the [[org.dbpedia.spotlight.model.OntologyType]] object corresponding to an RDF URI.
   *
   * @param url url representing the OntologyType object
   * @return
   */
  def getOntologyTypeByURL(url: String): OntologyType

}
