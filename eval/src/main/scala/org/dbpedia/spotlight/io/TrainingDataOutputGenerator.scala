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

/*class TopicalScoreDataGenerator(output: PrintWriter)  extends TSVOutputGenerator(output) {


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

class TopicalFilterDataGenerator(output: PrintWriter)  extends TSVOutputGenerator(output) {

    override def write(result: DisambiguationResult) {
        if (firstLine)
            header(List[String]("topicalScore","bestScore","difference","perplexity","keep"))

        if(result.predictedOccurrences.size >= 2) {
            val sum = result.predictedOccurrences.foldLeft(0.0)( _ + _.topicalScore)

            val log2 = math.log(2)
            val perplexity = math.pow(2,-result.predictedOccurrences.foldLeft(0.0)((acc,element)=> acc + element.topicalScore * math.log(element.topicalScore/sum)/log2)/sum)

            val correct = result.predictedOccurrences.find(_.resource.equals(result.correctOccurrence.resource)).getOrElse(null)
            if (correct==null)
                return

            var keep = true
            var first = true
            var bestScore = 0.0
            result.predictedOccurrences.sortBy(-_.topicalScore).foreach(occ => {
                if (first) {
                    bestScore = occ.topicalScore
                    first = false
                }

                val featureValues = List[String](occ.topicalScore.toString,bestScore.toString, (bestScore-occ.topicalScore).toString,perplexity.toString,keep.toString)
                output.append(line(featureValues))

                if (occ.resource.equals(result.correctOccurrence.resource))
                    keep = false
            })
        }
    }

}   */