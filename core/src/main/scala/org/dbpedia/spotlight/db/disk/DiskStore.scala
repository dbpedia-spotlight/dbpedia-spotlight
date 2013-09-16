package org.dbpedia.spotlight.db.disk

import java.io.File
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.db.model.TokenTypeStore

/**
 * Utility object for loading disk stores.
 *
 * @author Joachim Daiber
 */

object DiskStore {

  def loadContextStore(file: File, tokenTypeStore: TokenTypeStore): DiskContextStore = {
    SpotlightLog.info(this.getClass, "Opening disk-based context store...")
    val ds = new DiskContextStore(file.getAbsolutePath)
    ds.tokenTypeStore = tokenTypeStore

    ds
  }

}