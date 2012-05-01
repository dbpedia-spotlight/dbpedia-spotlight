package org.dbpedia.spotlight.spot.dictionary

import org.dbpedia.spotlight.util.bloomfilter.LongFastBloomFilter
import com.aliasi.util.AbstractExternalizable
import com.aliasi.dict.Dictionary
import java.io.{InputStream, File}

/**
 * This is a SurfaceForm dictionary using a Bloom filter. The properties of Bloom filters are very helpful
 * here. There will never be false negatives, but false positives may occur with a probability
 * that can be specified when creating the BloomFilter.
 */

class ProbabilisticSurfaceFormDictionary(
  expectedSize: Int,
  caseSensitive: Boolean = true,
  falsePositiveProbability: Double = 0.01
  ) extends SurfaceFormDictionary(caseSensitive) {

  val bloomFilter: LongFastBloomFilter =
    LongFastBloomFilter.getFilter(expectedSize, falsePositiveProbability)


  def add(surfaceForm: String) {
    bloomFilter.add(normalizeEntry(surfaceForm).getBytes)
  }


  def contains(surfaceForm: String) = bloomFilter.contains(normalizeEntry(surfaceForm).getBytes)


  def size = bloomFilter.getCurrentNumberOfElements.toInt
}

object ProbabilisticSurfaceFormDictionary {

  def fromInputStream(
    dictionaryFile: InputStream,
    caseSensitive: Boolean = true
  ): SurfaceFormDictionary = {
    SurfaceFormDictionary.fromIterator(io.Source.fromInputStream(dictionaryFile).getLines(),
      new ProbabilisticSurfaceFormDictionary(io.Source.fromInputStream(dictionaryFile).size, caseSensitive))
  }


  def fromLingPipeDictionary(
    dictionaryFile: File,
    caseSensitive: Boolean = true
  ): SurfaceFormDictionary = {
    val lingpipeDictionary: Dictionary[String] = AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]]
    SurfaceFormDictionary.fromLingPipeDictionary(lingpipeDictionary,
      new ProbabilisticSurfaceFormDictionary(lingpipeDictionary.size(), caseSensitive))
  }
}
