package org.dbpedia.spotlight.db.memory

import gnu.trove.TObjectIntHashMap
import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.db.model.SurfaceFormStore
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, ItemNotFoundException}
import com.esotericsoftware.kryo.Kryo
import scala.Array

/**
 * @author Joachim Daiber
 *
 *
 *
 */

@SerialVersionUID(1002001)
class MemorySurfaceFormStore
  extends MemoryStore
  with SurfaceFormStore {

  @transient
  var idForString: TObjectIntHashMap  = null

  var stringForID: Array[String]      = null
  var annotatedCountForID: Array[Int] = null
  var totalCountForID: Array[Int]     = null

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

    val annotatedCount = annotatedCountForID(id)
    val totalCount = totalCountForID(id)


    new SurfaceForm(surfaceform, id, annotatedCount, totalCount)
  }

}
