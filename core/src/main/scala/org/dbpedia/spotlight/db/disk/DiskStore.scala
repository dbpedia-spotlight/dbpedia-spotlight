package org.dbpedia.spotlight.db.disk

import java.io.File
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.db.model.TokenTypeStore

/**
 * Utility object for loading disk stores.
 *
 * @author Joachim Daiber
 */

object DiskStore {

  protected val LOG = LogFactory.getLog(this.getClass)

  def loadContextStore(file: File, tokenTypeStore: TokenTypeStore): DiskContextStore = {
    LOG.info("Opening disk-based context store...")
    val ds = new DiskContextStore(file.getAbsolutePath)
    ds.tokenTypeStore = tokenTypeStore

    ds
  }

}