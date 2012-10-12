package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException

/**
 * @author Joachim Daiber
 */

trait SurfaceFormStore {

  @throws(classOf[SurfaceFormNotFoundException])
  def getSurfaceForm(surfaceform: String): SurfaceForm

}
