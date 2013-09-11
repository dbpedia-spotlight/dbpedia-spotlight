package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.db.model.SurfaceFormStore
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import scala.Array
import java.lang.Integer
import util.StringToIDMapFactory

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
  var idForString: java.util.Map[String, Integer] = null

  var stringForID: Array[String]      = null
  var annotatedCountForID: Array[Int] = null
  var totalCountForID: Array[Int]     = null

  @transient
  var totalAnnotatedCount = 0

  @transient
  var totalOccurrenceCount = 0


  @transient
  var stopWords: Set[String] = Set("the", "an", "a")

  def normalize(sf: String): String =
    "/" + sf.replaceAll("[\\p{Punct}]+", " ").toLowerCase.split(" ").filter({lcSF: String => !stopWords.contains(lcSF)}).mkString(" ")

  override def loaded() {
    createReverseLookup()
  }

  def size = stringForID.size

  def getTotalAnnotatedCount: Int = totalAnnotatedCount
  def getTotalOccurrenceCount: Int = totalOccurrenceCount


  def iterateSurfaceForms: Seq[SurfaceForm] = {
    annotatedCountForID.zipWithIndex.flatMap{
      case (count: Int, id: Int) if count > 0 => Some(sfForID(id))
      case _ => None
    }
  }


  def createReverseLookup() {

    SpotlightLog.info(this.getClass, "Summing total SF counts.")
    totalAnnotatedCount = annotatedCountForID.sum
    totalOccurrenceCount = totalCountForID.sum


    if (stringForID != null) {
      SpotlightLog.info(this.getClass, "Creating reverse-lookup for surface forms, adding normalized surface forms.")
      idForString = StringToIDMapFactory.createDefault(stringForID.size * 2)

      var i = 0
      stringForID foreach { sf => {
        if (sf != null) {
          idForString.put(sf, i)

          val n = normalize(sf)
          if (idForString.get(n) == null || annotatedCountForID(idForString.get(n)) < annotatedCountForID(i))
            idForString.put(n, i)
        }
        i += 1
      }
      }
    }
  }


  private def sfForID(id: Int) = {
    val annotatedCount = annotatedCountForID(id)
    val totalCount = totalCountForID(id)

    new SurfaceForm(stringForID(id), id, annotatedCount, totalCount)
  }

  @throws(classOf[SurfaceFormNotFoundException])
  def getSurfaceForm(surfaceform: String): SurfaceForm = {
    val id = idForString.get(surfaceform)

    if (id == null)
      throw new SurfaceFormNotFoundException("SurfaceForm %s not found.".format(surfaceform))

    sfForID(id)
  }

  @throws(classOf[SurfaceFormNotFoundException])
  def getSurfaceFormNormalized(surfaceform: String): SurfaceForm = {
    val id = idForString.get(normalize(surfaceform))

    if (id == null)
      throw new SurfaceFormNotFoundException("SurfaceForm %s not found.".format(surfaceform))

    val annotatedCount = annotatedCountForID(id)
    val totalCount = totalCountForID(id)

    new SurfaceForm(surfaceform, id, annotatedCount, totalCount)
  }

}
