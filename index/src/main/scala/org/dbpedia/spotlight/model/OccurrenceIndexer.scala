package org.dbpedia.spotlight.model


/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait OccurrenceIndexer {

  def addOccurrence(occ: DBpediaResourceOccurrence)
  def addOccurrences(occs: Traversable[DBpediaResourceOccurrence])

}
