package org.dbpedia.spotlight.db.memory

import gnu.trove.TObjectIntHashMap
import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.db.model.SurfaceFormStore
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, ItemNotFoundException}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class MemorySurfaceFormStore
  extends MemoryStore
  with SurfaceFormStore {

  private val serialVersionUID = 101010103

  @transient
  var idForString: TObjectIntHashMap = null

  var stringForID: Array[String] = null
  var supportForID: Array[Int]   = null


  override def loaded() {
    createReverseLookup()
  }

  def size = stringForID.size

  def createReverseLookup() {
    if (stringForID != null) {
      System.err.println("Creating reverse-lookup for surface forms.")
      idForString = new TObjectIntHashMap(stringForID.size)

      var i = 0
      stringForID foreach { sf => {
        idForString.put(sf, i)
        i += 1
      }
      }
    }
  }


  @throws(classOf[SurfaceFormNotFoundException])
  def getSurfaceForm(surfaceform: String): SurfaceForm = {
    val id = idForString.get(surfaceform)

    if (id == 0)
      throw new SurfaceFormNotFoundException("SurfaceForm %s not found.".format(surfaceform))

    val support = supportForID(id)
    new SurfaceForm(surfaceform, id, support)
  }

}
