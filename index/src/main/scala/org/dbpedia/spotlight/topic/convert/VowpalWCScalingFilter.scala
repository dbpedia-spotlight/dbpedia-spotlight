package org.dbpedia.spotlight.topic.convert

import java.io.{FileWriter, PrintWriter}
import io.Source
import org.apache.commons.logging.LogFactory


/**
 * Simple class for scaling down word counts in a vowpal corpus
 * args: 1st input, 2nd output, 3rd scaling factor (new count = old count / factor) which has to be Integer
 */
object VowpalWCScalingFilter {

  private val LOG = LogFactory.getLog(getClass)

  def main(args:Array[String]) {
    reduceElementFeatures(args(0),args(1), args(2).toDouble)
  }

  def reduceElementFeatures(pathToCorpus:String, output:String, normalization:Double) {
    val writer = new PrintWriter(new FileWriter(output))
    var ctr = 0
    var split:Array[String] = null
    //var words:LinkedList[(Int,Long)] = null
    var count:Long = 0

    Source.fromFile(pathToCorpus).getLines().foreach( line => {
      writer.print("|")
      line.split(" ").drop(1).foreach ( word => {
        split = word.split(":")
        count = split(1).toLong
        if(count>normalization/2)
          writer.print(" "+split(0)+":"+math.round(count / normalization))
      })

      writer.println()

      ctr += 1
      if (ctr % 10000==0)
        LOG.info(ctr+" filtered examples written!")
    })
    writer.close()
  }

}
