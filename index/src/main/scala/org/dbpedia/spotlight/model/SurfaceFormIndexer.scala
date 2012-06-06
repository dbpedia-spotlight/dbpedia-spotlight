package org.dbpedia.spotlight.model

import java.util.List

/**
 * @author pablomendes
 * @author Joachim Daiber
 *
 */

trait SurfaceFormIndexer {

  def addSurfaceForm(sf: SurfaceForm, count: Int)
  def addSurfaceForms(sfCount: Map[SurfaceForm,Int])

}
