package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, Feature, DisambiguationResult}
import java.io.PrintWriter
import org.dbpedia.spotlight.exceptions.InputException

/**
 *
 * @author pablomendes
 */

abstract class TrainingDataOutputGenerator(output: PrintWriter) extends TSVOutputGenerator(output) {

    /**
     * Should be implemented for different disambiguators
     *
     * @param result
     * @return
     */
    protected def extractFeatures(result: DisambiguationResult) : List[Feature]

    override def write(result: DisambiguationResult) {
        val correct = if (result.rank==1) "1" else "0"

        result.predicted match {
            case Some(r) => {
                val features = extractFeatures(result)
                if (firstLine)
                    header(features.map(_.featureName) ::: List("class"))
                val featureValues = features.map(_.toString) :::  List(correct)
                output.append(line(featureValues))
            }
            case None => // ignore
        }
    }

}

class ProbabilityTrainingData(output: PrintWriter) extends TrainingDataOutputGenerator(output) {


    /**
     * Should be implemented for different disambiguators. Assumes that all needed features are there.
     *
     * @param result
     * @return
     */
    protected def extractFeatures(result: DisambiguationResult) = {
        val r = result.predicted.get // only called when this is there

        try {
            List(r.feature("candidatePrior"), r.feature("contextualScore")).map(_.get)   //get feature and scream if it cannot find
        }
        catch {
            case e: NoSuchElementException =>  throw new InputException("DisambiguationResult did not contain one of the expected features.")
        }
    }

}

class TopicalTrainingDataGenerator(output: PrintWriter)  extends TSVOutputGenerator(output) {


    protected def extractFeatures(r: DBpediaResourceOccurrence) = {
        try {
            List(r.feature("contextualScore"), r.feature("topicalScore")).map(_.get)   //get feature and scream if it cannot find
        }
        catch {
            case e: NoSuchElementException =>  throw new InputException("DisambiguationResult did not contain one of the expected features.")
        }
    }

    override def write(result: DisambiguationResult) {
        result.predictedOccurrences.foreach( occurrence => {
            val correct = if (occurrence.resource.equals(result.correctOccurrence.resource)) "1" else "0"

            val features = extractFeatures(occurrence)
            if (firstLine)
                header(features.map(_.featureName) ::: List("class"))
            val featureValues = features.map(_.toString) :::  List(correct)
            output.append(line(featureValues))
        } )
    }

}