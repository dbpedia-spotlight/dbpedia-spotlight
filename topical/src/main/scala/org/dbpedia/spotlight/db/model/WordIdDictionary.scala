package org.dbpedia.spotlight.db.model

import java.io._
import collection.mutable._
import io.Source

/**
 * Utility class which handles all word to id dictionary related functions.
 *
 * @param dictionaryFile
 * @param maxSize maximal allowed size of this dictionary
 * @param isPhonetic if dictionary contains phonetics as entries
 *
 * @author dirk
 */

class WordIdDictionary(private val dictionaryFile: File, private var maxSize: Int, var isPhonetic: Boolean = false) {
  def this(dictionaryFile: File) = this(dictionaryFile, 0)

  private val LOG = LogFactory.getLog(getClass)

  private var dictionary: Map[String, Int] = Map()

  {
    try {
      var firstLine = true
      Source.fromFile(dictionaryFile).getLines().foreach(thisLine => {
        if (firstLine) {
          if (thisLine.trim.equals("@phonetic"))
            isPhonetic = true
          else {
            val split = thisLine.split("\t")
            dictionary += (split(0) -> split(1).toInt)
          }
          firstLine = false
        }
        else {
          val split = thisLine.split("\t")
          dictionary += (split(0) -> split(1).toInt)
        }
      })
      LOG.info("Dictionary loaded")
    }
    catch {
      case e: FileNotFoundException => {
        LOG.info("New dictionary created")
      }
      case e: IOException => {
        e.printStackTrace
      }
    }
  }

  def persist() {
    try {
      val pw: PrintWriter = new PrintWriter(new FileWriter(dictionaryFile))
      if (isPhonetic)
        pw.println("@phonetic")

      dictionary.foreach((entry) => pw.println(entry._1 + "\t" + entry._2))
      pw.flush
      pw.close
    }
    catch {
      case e: IOException => {
        e.printStackTrace
      }
    }
  }

  def getId(word: String): Int = dictionary.getOrElse(word, -1)

  def put(word: String, id: Int) {
    dictionary += (word -> id)
  }

  def getOrElsePut(word: String): Int = {
    var id = getId(word)
    if (id < 0 && getSpace > 0) {
      id = getSize
      put(word, id)
    }

    id
  }

  def getDictionary = dictionary

  def getSize = dictionary.size

  def getMaxSize = if (maxSize == 0) getSize else maxSize

  def setMaxSize(maxSize: Int) {
    this.maxSize = maxSize
  }

  def getSpace = maxSize - dictionary.size

}
