package org.dbpedia.spotlight.spot.opennlp

import java.io.File
import collection.mutable.HashSet
import org.dbpedia.spotlight.util.bloomfilter.LongFastBloomFilter
import com.aliasi.util.AbstractExternalizable
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
  def normalizeEntry(entry: String) = if(caseSensitive) entry else entry.toLowerCase
  def size: Int
}

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

/**
 * This is a SurfaceForm dictionary using a Bloom filter. The properties of Bloom filters are very helpful
 * here. There will never be false negatives, but false positives may occur with a probability
 * that can be specified when creating the BloomFilter.
 */
class ProbabilisticSurfaceFormDictionary(expectedSize: Int, caseSensitive: Boolean = true, falsePositiveProbability: Double = 0.01)
  extends SurfaceFormDictionary(caseSensitive) {

  val bloomFilter: LongFastBloomFilter =
    LongFastBloomFilter.getFilter(expectedSize, falsePositiveProbability)

  def add(surfaceForm: String) {
    bloomFilter.add(normalizeEntry(surfaceForm).getBytes)
  }
  def contains(surfaceForm: String) = bloomFilter.contains(normalizeEntry(surfaceForm).getBytes)
  def size = bloomFilter.getCurrentNumberOfElements.toInt
}


/**
 * Simple factory methods for each of the SurfaceFormDictionary implementations.
 */
object SurfaceFormDictionary {
  def fromIterator(entries: scala.collection.Iterator[String],
                   surfaceformDictionary: SurfaceFormDictionary = new ExactSurfaceFormDictionary())
    : SurfaceFormDictionary = {

    entries.foreach(line => surfaceformDictionary.add(line))
    surfaceformDictionary
  }
  def fromLingPipeDictionary(dictionary: Dictionary[String],
                             surfaceformDictionary: SurfaceFormDictionary = new ExactSurfaceFormDictionary()) = {

    dictionary.entryList().foreach(entry => surfaceformDictionary.add(entry.phrase()))
    surfaceformDictionary
  }

}

object ProbabilisticSurfaceFormDictionary {
  def fromFile(dictionaryFile: File, caseSensitive: Boolean = true) : SurfaceFormDictionary = {
    SurfaceFormDictionary.fromIterator(io.Source.fromFile(dictionaryFile).getLines(),
      new ProbabilisticSurfaceFormDictionary(io.Source.fromFile(dictionaryFile).size, caseSensitive))
  }
  def fromLingPipeDictionaryFile(dictionaryFile: File, caseSensitive: Boolean = true): SurfaceFormDictionary = {
    val lingpipeDictionary: Dictionary[String] = AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]]
    SurfaceFormDictionary.fromLingPipeDictionary(lingpipeDictionary,
      new ProbabilisticSurfaceFormDictionary(lingpipeDictionary.size(), caseSensitive))
  }
    def fromLingPipeDictionary(lingpipeDictionary: Dictionary[String], caseSensitive: Boolean = true): SurfaceFormDictionary = {
        SurfaceFormDictionary.fromLingPipeDictionary(lingpipeDictionary,
            new ProbabilisticSurfaceFormDictionary(lingpipeDictionary.size(), caseSensitive))
    }
}

object ExactSurfaceFormDictionary {
  def fromFile(dictionaryFile: File, caseSensitive: Boolean = true) : SurfaceFormDictionary = {
    SurfaceFormDictionary.fromIterator(io.Source.fromFile(dictionaryFile).getLines(),
      new ExactSurfaceFormDictionary(caseSensitive))
  }
  def fromLingPipeDictionary(dictionaryFile: File, caseSensitive: Boolean = true) : SurfaceFormDictionary = {
    SurfaceFormDictionary.fromLingPipeDictionary(AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]],
      new ExactSurfaceFormDictionary(caseSensitive))
  }
}



