package org.dbpedia.spotlight.db.disk

import org.dbpedia.spotlight.model.{Token, DBpediaResource, SurfaceForm}
import org.dbpedia.spotlight.db.model.{TokenStore, ResourceStore, SurfaceFormStore}
import org.dbpedia.spotlight.db.Containers.SFContainer

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object DiskBasedStores {

  class DiskBasedSurfaceFormStore(file: String) extends SurfaceFormStore {

    val jdbm = new JDBMStore[String, SFContainer](file)

    def get(surfaceform: String): SurfaceForm = {
      val sfc = jdbm.get(surfaceform)
      if (sfc == null)
        return null
      new SurfaceForm(surfaceform, sfc.id, sfc.support)
    }

  }

  class DiskBasedResourceStore(file: String) extends JDBMStore[Int, DBpediaResource](file) with ResourceStore
  class DiskBasedTokenStore(file: String) extends JDBMStore[String, Token](file) with TokenStore

}
