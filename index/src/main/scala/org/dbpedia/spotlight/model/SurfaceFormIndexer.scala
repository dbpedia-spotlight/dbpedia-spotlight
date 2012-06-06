package org.dbpedia.spotlight.model

import java.util.List

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait SurfaceFormIndexer {

  def add(sf: SurfaceForm, count: Int)
  def add(sfCount: Map[SurfaceForm,Int])

}
