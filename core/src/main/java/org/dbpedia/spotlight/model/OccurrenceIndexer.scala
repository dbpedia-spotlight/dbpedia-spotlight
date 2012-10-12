package org.dbpedia.spotlight.model

import java.util.List

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait OccurrenceIndexer {

  def add(p: AnnotatedParagraph)

  def add(pars: List[AnnotatedParagraph])

}
