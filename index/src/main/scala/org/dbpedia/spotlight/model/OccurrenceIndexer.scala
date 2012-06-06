package org.dbpedia.spotlight.model

import java.util.List

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait OccurrenceIndexer {

  def add(occ: DBpediaResourceOccurrence)
  def add(occs: List[DBpediaResourceOccurrence])

}
