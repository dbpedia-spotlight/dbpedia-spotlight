package org.dbpedia.spotlight.db.disk

import org.dbpedia.spotlight.db.model.SurfaceFormStore
import org.dbpedia.spotlight.model.SurfaceForm

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class DiskSurfaceFormStore(file: String) extends SurfaceFormStore {

  val jdbm = new JDBMStore[String, Triple[Int, Int, Int]](file)

  def getSurfaceForm(surfaceform: String): SurfaceForm = {
    val sfc = jdbm.get(surfaceform)
    if (sfc == null)
      return null
    new SurfaceForm(surfaceform, sfc._1, sfc._2, sfc._3)
  }

}