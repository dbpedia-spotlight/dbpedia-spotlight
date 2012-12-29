package org.dbpedia.spotlight.topical.index

import scala.{Boolean, Double}
import java.io.{FileWriter, PrintWriter, File}
import org.dbpedia.spotlight.model.{Topic, TopicalClassificationConfiguration}
import org.dbpedia.spotlight.topical.{TopicalClassifierLoader, WekaMultiLabelClassifier, WekaSingleLabelClassifier, TopicalClassifier}
import org.dbpedia.spotlight.topical.util.TopicUtil
import org.dbpedia.spotlight.io.FileOccurrenceSource
import org.apache.commons.logging.LogFactory
import collection.mutable._

/**
 * This object takes any occs.tsv file and splits it with the specified topical classification configuration into topics to the output
 * directory similar to the SplitOccs* classes. It is able to append new assignments to existing splits.
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
        val config = new TopicalClassificationConfiguration(args(1))


        assignTopics(new File(args(0)), TopicalClassifierLoader.fromConfig(config), args(2).toDouble, new File(args(3)), args(4).toBoolean)
    }

    def assignTopics(occsFile: File, model: TopicalClassifier, minimalConfidence: Double, output: File, append: Boolean) {
        val writers = Map[Topic, PrintWriter]()

        model.getTopics.foreach(topic => writers += (topic -> new PrintWriter(new FileWriter(new File(output, topic.getName + ".tsv"), append))))
        val otherWriter = new PrintWriter(new FileWriter(new File(output, TopicUtil.CATCH_TOPIC.getName + ".tsv"), append))
        var predictions: Array[(Topic, Double)] = null
        var written = false

        var ctr = 0
        var assignments = 0
        FileOccurrenceSource.fromFile(occsFile).foreach(occ => {
            predictions = model.getPredictions(occ.context)
            written = false
            predictions.foreach {
                case (topic, prediction) => {
                    if (prediction >= minimalConfidence) {
                        writers(topic).println(occ.toTsvString)
                        written = true
                        assignments += 1
                        if (assignments % 10000 == 0)
                            LOG.info(assignments + "-th assignment: " + occ.id + ", " + occ.resource.uri + "->" + topic.getName)
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
