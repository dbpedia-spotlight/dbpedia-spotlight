package org.dbpedia.spotlight.model

import java.util.Map

/**
 * @author pablomendes
 * @author Joachim Daiber
 */

trait SurfaceFormIndexer {

  /**
   * Adds the [[org.dbpedia.spotlight.model.SurfaceForm]] with with the corresponding annotated count and
   * total count. Total count is the number of times the surface form was observed,
   * whether annotated or not.
   *
   * @param sf the surface form
   * @param annotatedCount count of annotated occurrences of the surface form
   * @param totalCount count of total occurrences of the surface form
   */
  def addSurfaceForm(sf: SurfaceForm, annotatedCount: Int, totalCount: Int)


  /**
   * Adds every [[org.dbpedia.spotlight.model.SurfaceForm]] in the Map with its
   * corresponding annotated and total count.
   *
   * @param sfCount Map from SurfaceForms to their annotated and total counts
   */
  def addSurfaceForms(sfCount: Map[SurfaceForm, (Int, Int)])

}
