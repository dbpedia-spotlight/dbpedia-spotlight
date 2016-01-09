package org.dbpedia.spotlight.db.io.ranklib

import java.io.{PrintWriter, OutputStream}

import org.dbpedia.spotlight.db.SpotlightModel
import org.dbpedia.spotlight.log.SpotlightLog

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

        val featNames = SpotlightModel.featureNames
        val feats = featNames.map(occ.featureValue[Double](_).get)


        def isValid(feature: Double) = !(feature.isNaN || feature.isInfinity || feature.isNegInfinity)

        if (feats.map(isValid).reduce(_ && _)) {
          val out = "%s qid:%s %s".format(
            rank,
            qid,
            feats.zipWithIndex.map{
              case (feat, idx) =>
                "%s:%s".format(idx + 1, feat)
            }.reduce(_ + " " + _)
          )
          output.println(out)
          output.flush()
        }else{
          println("Warning: Invalid feature detected for resource %s".format(occ.resource.getFullUri))
        }
      }
    }else{
      //println("Resource %s not found in predictions (%s)!".format(result.correctOccurrence.resource.getFullUri, result.predictedOccurrences.map(_.resource.getFullUri).reduce(_ + ", " + _)))
      try {
        SpotlightLog.debug(this.getClass, "Resource %s not found in predictions (%s)!".format(result.correctOccurrence.resource.getFullUri, result.predictedOccurrences.map(_.resource.getFullUri).reduce(_ + ", " + _)))
      }catch {
        case _ : Throwable => SpotlightLog.debug(this.getClass, "No prediction for resource %s".format(result.correctOccurrence.resource.getFullUri))
      }
    }
  }

}
