package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.db.model.SurfaceFormStore
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import scala.Array
import java.lang.Integer
import util.StringToIDMapFactory
import scala.collection.mutable
import org.apache.commons.lang.StringUtils

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

  var lowercaseMap: java.util.HashMap[String, Array[Int]] = null
  var stringForID: Array[String]      = null
  var annotatedCountForID: Array[Short] = null
  var totalCountForID: Array[Short]     = null

  @transient
  var totalAnnotatedCount = 0

  @transient
  var totalOccurrenceCount = 0


  @transient
  var stopWords: Set[String] = Set("the", "an", "a")

  def normalize(sf: String): String = sf.replaceAll("[\\p{Punct}]+", " ").toLowerCase.split(" ").filter({lcSF: String => !stopWords.contains(lcSF)}).mkString(" ")

  override def loaded() {
    createReverseLookup()
  }

  def size = stringForID.size

  def getTotalAnnotatedCount: Int = totalAnnotatedCount
  def getTotalOccurrenceCount: Int = totalOccurrenceCount


  def iterateSurfaceForms: Seq[SurfaceForm] = {
    annotatedCountForID.zipWithIndex.flatMap{
      case (count: Short, id: Int) if qc(count) > 0 => Some(sfForID(id))
      case _ => None
    }
  }


  def createReverseLookup() {

    SpotlightLog.info(this.getClass, "Summing total SF counts.")
    totalAnnotatedCount = annotatedCountForID.map(q => qc(q)).sum
    totalOccurrenceCount = totalCountForID.map(q => qc(q)).sum


    if (stringForID != null) {
      SpotlightLog.info(this.getClass, "Creating reverse-lookup for surface forms, adding normalized surface forms.")
      idForString = StringToIDMapFactory.createDefault(stringForID.size * 2)

      var i = 0
      stringForID foreach { sf =>
        if (sf != null) {
          idForString.put(sf, i)
        }
        i += 1
      }
    }
  }


  private def sfForID(id: Int) = {
    val annotatedCount = qc(annotatedCountForID(id))
    val totalCount = qc(totalCountForID(id))

    new SurfaceForm(stringForID(id), id, annotatedCount, totalCount)
  }

  @throws(classOf[SurfaceFormNotFoundException])
  def getSurfaceForm(surfaceform: String): SurfaceForm = {
    val id = idForString.get(surfaceform)

    if (id == null)
      throw new SurfaceFormNotFoundException("SurfaceForm %s not found.".format(surfaceform))

    sfForID(id)
  }

  private def getLowercaseCandidateList(surfaceform: String): Array[Int] = {
    val cs = lowercaseMap.get(surfaceform.toLowerCase)

    if(cs != null && cs.size > 1)
      cs.tail.toArray
    else
      Array[Int]()
  }

  def getSurfaceFormsNormalized(surfaceform: String): Set[SurfaceForm] = {
    val ls = getLowercaseCandidateList(surfaceform)
    ls.map( id => sfForID(id) ).toSet
  }

  @throws(classOf[SurfaceFormNotFoundException])
  def getSurfaceFormNormalized(surfaceform: String): SurfaceForm = {
    val sfs = getRankedSurfaceFormCandidates(surfaceform)

    if (sfs.isEmpty)
      throw new SurfaceFormNotFoundException(surfaceform)
    else
      sfs.head._1
  }


  private def editDistanceScore(sData: String, sReal: String): Double = {
    val ed = StringUtils.getLevenshteinDistance(sData, sReal)

    if (sReal.equals(sData))
      1.0
    else if (sData.toUpperCase.equals(sReal) || sData.toLowerCase.equals(sReal))
      0.85
    else
      0.85 * (1.0 - (ed / sReal.length.toDouble))
  }

  def getRankedSurfaceFormCandidates(surfaceform: String): Seq[(SurfaceForm, Double)] = {

    getSurfaceFormsNormalized(surfaceform).map{ candSf: SurfaceForm =>
      val cLower = getLowercaseSurfaceFormCount(surfaceform.toLowerCase)
      val cTotal = candSf.totalCount

      SpotlightLog.debug(this.getClass, surfaceform + " p: "+ candSf.annotationProbability)
      SpotlightLog.debug(this.getClass, surfaceform + " edit distance: "+ editDistanceScore(candSf.name, surfaceform))
      SpotlightLog.debug(this.getClass, surfaceform + " c in total: "+ cTotal.toDouble / (cLower+cTotal))

      (candSf,
        //Score for the surface form (including the case adaptation):
        editDistanceScore(candSf.name, surfaceform) *
        candSf.annotationProbability *
        ((2.0 * cTotal.toDouble) / (cLower+cTotal))
      )
    }.toSeq.sortBy(-_._2)

  }


  /**
   * Get the count of the lowercase version of a surface form (for working with ill-cased text).
   *
   * @param surfaceform the queried surface form
   * @return
   */
  def getLowercaseSurfaceFormCount(surfaceform: String): Int = lowercaseMap.get(surfaceform).headOption match {
    case Some(c) => c
    case _ => 0
  }

}
