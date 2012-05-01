package org.dbpedia.spotlight.spot.dictionary

import collection.mutable.HashSet
import com.aliasi.util.AbstractExternalizable
import com.aliasi.dict.Dictionary
import java.io.{InputStream, File}

/**
 * This is a SurfaceForm dictionary using a HashSet, hence it is fully reliable but requires more space
 * than a fuzzy dictionary.
 */

class ExactSurfaceFormDictionary(caseSensitive: Boolean = true) extends SurfaceFormDictionary(caseSensitive) {

  val surfaceFormDictionary = new HashSet[String]

  def add(surfaceForm: String) {
    surfaceFormDictionary += normalizeEntry(surfaceForm)
  }

  def contains(surfaceForm: String): Boolean = surfaceFormDictionary.contains(normalizeEntry(surfaceForm))


  def size = surfaceFormDictionary.size
}

object ExactSurfaceFormDictionary {

  def fromInputStream(
    dictionaryFile: InputStream,
    caseSensitive: Boolean = true
  ): SurfaceFormDictionary = {
    SurfaceFormDictionary.fromIterator(io.Source.fromInputStream(dictionaryFile).getLines(),
      new ExactSurfaceFormDictionary(caseSensitive))
  }


  def fromLingPipeDictionary(
    dictionaryFile: File,
    caseSensitive: Boolean = true
  ): SurfaceFormDictionary = {
    SurfaceFormDictionary.fromLingPipeDictionary(AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]],
      new ExactSurfaceFormDictionary(caseSensitive))
  }
}
