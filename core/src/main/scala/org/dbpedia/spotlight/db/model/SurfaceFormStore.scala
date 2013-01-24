package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import scala.throws

/**
 *
 * @author Joachim Daiber
 */

trait SurfaceFormStore {


  /**
   * Get the [[org.dbpedia.spotlight.model.SurfaceForm]] object corresponding to the
   * String. If the surface form is not known, an exception is thrown.
   *
   * @param surfaceform the queried surface form
   * @throws org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
   * @return
   */
  @throws(classOf[SurfaceFormNotFoundException])
  def getSurfaceForm(surfaceform: String): SurfaceForm


  /**
   * Attempt to find a [[org.dbpedia.spotlight.model.SurfaceForm]] for the String by
   * first normalizing it (removing stop words, punctuation, lowercasing).
   *
   * @param surfaceform the queried surface form
   * @throws org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
   * @return
   */
  @throws(classOf[SurfaceFormNotFoundException])
  def getSurfaceFormNormalized(surfaceform: String): SurfaceForm


  /**
   * Returns the total annotated count of all [[org.dbpedia.spotlight.model.SurfaceForm]]s in the store.
   *
   * @return
   */
  def getTotalAnnotatedCount: Int

  /**
   * Returns the total occurrence count of all [[org.dbpedia.spotlight.model.SurfaceForm]]s in the store.
   *
   * @return
   */
  def getTotalOccurrenceCount: Int

}
