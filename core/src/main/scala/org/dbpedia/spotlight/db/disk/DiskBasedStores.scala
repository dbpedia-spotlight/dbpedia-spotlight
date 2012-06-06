package org.dbpedia.spotlight.db.disk

import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.db.model.SurfaceFormStore

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object DiskBasedStores {

  class DiskBasedSurfaceFormStore(file: String) extends SurfaceFormStore {

    val jdbm = new JDBMStore[String, Pair[Int, Int]](file)

    def get(surfaceform: String): SurfaceForm = {
      val sfc = jdbm.get(surfaceform)
      if (sfc == null)
        return null
      new SurfaceForm(surfaceform, sfc._1, sfc._2)
    }

  }


}
