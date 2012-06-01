package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.SurfaceForm

/**
 * @author Joachim Daiber
 */

trait SurfaceFormStore {

  def get(surfaceform: String): SurfaceForm

}
