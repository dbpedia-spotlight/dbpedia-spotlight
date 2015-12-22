package org.dbpedia.spotlight.db.io.ranklib

import java.io.{PrintWriter, OutputStream}

/**
 * Created by dowling on 02/08/15.
 */
class RanklibTrainingDataWriter(output: PrintWriter) {
  var qid = 0
  def write(result: TrainingDataEntry): Unit = {
    qid += 1
    var rank = 0

    // skip the sample if the correct entity is not among predictions
    if (result.predictedOccurrences.map(_.resource).exists(res => res.getFullUri equals result.correctOccurrence.resource.getFullUri)) {
      result.predictedOccurrences.foreach { occ =>

        if (occ.resource.getFullUri equals result.correctOccurrence.resource.getFullUri)
          rank = 2
        else
          rank = 1

        val (f1, f2, f3) = (
          occ.featureValue[Double]("P(s|e)").get,
          occ.featureValue[Double]("P(c|e)").get,
          occ.featureValue[Double]("P(e)").get
          )

        def isValid(feature: Double) = !(feature.isNaN || feature.isInfinity || feature.isNegInfinity)

        if (isValid(f1) && isValid(f2)&& isValid(f3)) {
          val out = "%s qid:%s 1:%s 2:%s 3:%s".format(
            rank,
            qid,
            f1,
            f2,
            f3
          )
          // println("Writing "+out)


          output.println(out)
          output.flush()
        }else{
          println("Warning: Invalid feature detected for resource %s".format(occ.resource.getFullUri))
        }
      }
    }else{
      //println("Resource %s not found in predictions (%s)!".format(result.correctOccurrence.resource.getFullUri, result.predictedOccurrences.map(_.resource.getFullUri).reduce(_ + ", " + _)))
      try {
        println("Resource %s not found in predictions (%s)!".format(result.correctOccurrence.resource.getFullUri, result.predictedOccurrences.map(_.resource.getFullUri).reduce(_ + ", " + _)))
      }catch {
        case _ : Throwable => println("No prediction for resource %s".format(result.correctOccurrence.resource.getFullUri))
      }
    }
  }

}
