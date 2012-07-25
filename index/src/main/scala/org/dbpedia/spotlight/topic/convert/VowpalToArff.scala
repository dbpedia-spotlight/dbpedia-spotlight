package org.dbpedia.spotlight.topic.convert

import org.dbpedia.spotlight.topic.utility.WordIdDictionary
import java.io.{FileWriter, PrintWriter}
import org.dbpedia.spotlight.model.Topic
import scala.collection.mutable._
import org.apache.commons.logging.LogFactory

/**
 * Utility class which converts vowpal wabbit input format + specified label file (1st line: comma-separated list of labels,
 * next: each line is one label, label on 2nd line refers to 1st corpus element etc) to .arff file
 */

object VowpalToArff {
  private val LOG = LogFactory.getLog(getClass)

  def writeVowpalToArff(dictionary:WordIdDictionary, inputCorpusPath:String, inputCorpusLabelPath:String,outputPath:String, normalize:Boolean) {
    val lines = scala.io.Source.fromFile(inputCorpusPath).getLines()
    val topicLines = scala.io.Source.fromFile(inputCorpusLabelPath).getLines()

    var split : Array[String] = null
    var word : Array[String] = null
    var wordId = -1
    var values : List[(Int,Double)] = List()
    var topicsLine = ""

    val pw : PrintWriter = new PrintWriter(new FileWriter(outputPath))
    var ctr = 0

    pw.println("@RELATION topics")
    for (i <- 0 until dictionary.getSize)
      pw.println("@ATTRIBUTE word_"+i+" NUMERIC")

    val tops : Array[String] = topicLines.next().split(",")
    if (tops.length>10000)
      pw.println("@ATTRIBUTE class string")
    else
      pw.println("@ATTRIBUTE class {"+tops.sorted.reduceLeft((acc, topic) => acc + "," + topic)+"}")
    pw.println("@DATA")

    lines.foreach((fileLine) => {
      topicsLine = topicLines.next()
      if (!topicsLine.equals("_ignore")) {
        values = List()

        split = fileLine.split(" ")

        if(split.length > 20) {
          for(i <- 1 until split.length) {
            word = split(i).split(":")

            wordId = word(0).toInt//dictionary.getDictionary.getOrElse(word(0),-1)
            if (wordId > -1)  {
              values = (wordId, word(1).toDouble) :: values
            }
          }
          //length normalization
          var squaredSum = 1.0
          if (normalize)
            squaredSum = math.sqrt(values.foldLeft(0.0)( (acc,element) => acc + element._2*element._2))

          values = values.sortBy(_._1)

          topicsLine.split(",").foreach(topic => {
            pw.print("{")
            values.foreach( element => pw.print(element._1+" "+ (math.round(element._2 / squaredSum * 1000.0)/1000.0) +",") )
            pw.println(dictionary.getSize+" "+topic+"}")
          })

          ctr +=1
          if( ctr%10000 == 0)
            LOG.info(ctr+ " examples written")
        }
      }
    })
    pw.flush()
    pw.close()
  }
}
