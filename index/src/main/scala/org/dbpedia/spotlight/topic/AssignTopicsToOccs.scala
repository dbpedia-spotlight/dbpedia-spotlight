package org.dbpedia.spotlight.topic

import org.dbpedia.spotlight.io.FileOccurrenceSource
import scala.collection.mutable._
import java.io.{FileWriter, PrintWriter, File}
import org.dbpedia.spotlight.model.{TopicalClassificationConfiguration, Topic}
import org.apache.commons.logging.LogFactory
import util.TopicUtil

/**
 * This object takes any occs.tsv file and splits it with the specified topical classification configuration into topics to the output
 * directory similar to the SplitOccs* classes.
 *
 * @see SplitOccsByCategories$
 *
 * @author Dirk Weißenborn
 */
object AssignTopicsToOccs {

  private val LOG = LogFactory.getLog(getClass)

  /**
   *
   * @param args 1st: path to input occs, 2nd: path to topical classification configuration file,
   *             3rd: min confidence of assigning, 4th: output, 5th: append (true|false)
   */
  def main(args: Array[String]) {
    assignTopics(args(0), new TopicalClassificationConfiguration(args(1)).getClassifier, args(2).toDouble, args(3),args(4).toBoolean)
  }

  def assignTopics(pathToOccsFile: String, model:TopicalClassifier, minimalConfidence: Double, outputPath: String, append:Boolean) {
    val writers = Map[Topic, PrintWriter]()

    model.getTopics.foreach(topic => writers += (topic -> new PrintWriter(new FileWriter(outputPath + "/" + topic.getName + ".tsv",append))))
    val otherWriter = new PrintWriter(new FileWriter(outputPath + TopicUtil.CATCH_TOPIC.getName +".tsv",append))
    var predictions: Array[(Topic, Double)] = null
    var written = false

    var ctr = 0
    var assignments = 0
    FileOccurrenceSource.fromFile(new File(pathToOccsFile)).foreach(occ => {
      predictions = model.getPredictions(occ.context)
      written = false
      predictions.foreach {
        case (topic, prediction) => {
          if (prediction >= minimalConfidence) {
            writers(topic).println(occ.toTsvString)
            written = true
            assignments += 1
            if (assignments % 10000 == 0)
              LOG.info(assignments+"-th assignment: "+occ.resource.uri+"->"+topic.getName)
          }
        }
      }
      if (!written)
        otherWriter.println(occ.toTsvString)

      ctr += 1
      if (ctr % 100000 == 0)
        LOG.info(ctr + " occs processed")
    })

    writers.foreach(_._2.close())
    otherWriter.close()
  }

}
