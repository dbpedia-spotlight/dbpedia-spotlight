package org.dbpedia.spotlight.topic.utility

import org.apache.commons.logging.LogFactory
import java.io._
import collection.mutable._
import io.Source

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 6/13/12
 * Time: 4:33 PM
 * To change this template use File | Settings | File Templates.
 */

class WordIdDictionary(private val pathToDictionary: String, private var maxSize:Int, var isPhonetic:Boolean = false) {
  def this(pathToDic:String)=this(pathToDic,0)

  private val LOG = LogFactory.getLog(getClass)

  private var dictionary: Map[String, Int] = Map()

  {
    try {
      var firstLine = true
      Source.fromFile(pathToDictionary).getLines().foreach(thisLine => {
        if(firstLine) {
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
      } )
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
      val pw: PrintWriter = new PrintWriter(new FileWriter(pathToDictionary))
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

  def getId(word: String): Int = dictionary.getOrElse(word,-1)

  def put(word: String, id: Int) {
    dictionary += (word -> id)
  }

  def getOrElsePut(word:String) : Int = {
    var id = getId(word)
    if (id < 0 && getSpace > 0) {
      id = getSize
      put(word, id)
    }

    id
  }

  def getDictionary = dictionary

  def getSize = dictionary.size

  def getMaxSize = if(maxSize==0) getSize else maxSize
  def setMaxSize(maxSize:Int) { this.maxSize = maxSize }

  def getSpace = maxSize - dictionary.size

}
