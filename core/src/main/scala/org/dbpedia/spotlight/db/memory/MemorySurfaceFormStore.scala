package org.dbpedia.spotlight.db.memory

import gnu.trove.TObjectIntHashMap
import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.db.model.SurfaceFormStore

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class MemorySurfaceFormStore
  extends MemoryStore
  with SurfaceFormStore {

  var idForString: TObjectIntHashMap = null
  var supportForID: Array[Int] = null

  def getSurfaceForm(surfaceform: String): SurfaceForm = {
    val id = idForString.get(surfaceform)
    val support = supportForID(id)
    new SurfaceForm(surfaceform, id, support)
  }

}
