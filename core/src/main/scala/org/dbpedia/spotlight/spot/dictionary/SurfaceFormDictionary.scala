package org.dbpedia.spotlight.spot.dictionary

import com.aliasi.dict.Dictionary
import scala.collection.JavaConversions._

/**
 * @author Joachim Daiber
 */


/**
 * A surface form dictionary (or set, lookup) can be used to check if a surface form
 * is known to be a valid spot candidate.
 */
abstract class SurfaceFormDictionary(caseSensitive: Boolean = true) {

  def contains(surfaceForm: String): Boolean
  def add(surfaceForm: String)
  def normalizeEntry(entry: String) = if (caseSensitive) entry else entry.toLowerCase
  def size: Int
}


/**
 * Simple factory methods for each of the SurfaceFormDictionary implementations.
 */
object SurfaceFormDictionary {

  def fromIterator(
    entries: scala.collection.Iterator[String],
    surfaceformDictionary: SurfaceFormDictionary = new ExactSurfaceFormDictionary()
  ): SurfaceFormDictionary = {

    entries.foreach(line => surfaceformDictionary.add(line))
    surfaceformDictionary
  }


  def fromLingPipeDictionary(
    dictionary: Dictionary[String],
    surfaceformDictionary: SurfaceFormDictionary = new ExactSurfaceFormDictionary()
  ) = {

    dictionary.entryList().foreach(entry => surfaceformDictionary.add(entry.phrase()))
    surfaceformDictionary
  }

}
