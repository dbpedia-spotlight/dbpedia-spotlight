package org.dbpedia.spotlight.db.disk

import org.dbpedia.spotlight.model.{Token, DBpediaResource, SurfaceForm}
import org.dbpedia.spotlight.db.model.{TokenStore, ResourceStore, SurfaceFormStore}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object DiskBasedStores {

  class DiskBasedSurfaceFormStore(file: String) extends JDBMStore[String, SurfaceForm](file) with SurfaceFormStore
  class DiskBasedResourceStore(file: String) extends JDBMStore[Int, DBpediaResource](file) with ResourceStore
  class DiskBasedTokenStore(file: String) extends JDBMStore[String, Token](file) with TokenStore

}
