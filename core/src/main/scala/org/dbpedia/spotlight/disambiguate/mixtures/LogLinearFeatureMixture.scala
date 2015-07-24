package org.dbpedia.spotlight.disambiguate.mixtures

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import org.dbpedia.spotlight.util.MathUtil

/**
 * Created by dowling on 24/07/15.
 */
class LogLinearFeatureMixture(weightedFeatures: List[Pair[String, Double]]) extends Mixture(0){
  override def getScore(occurrence: DBpediaResourceOccurrence): Double = {
    MathUtil.lnproduct(weightedFeatures.map{ case (f, w) =>
      occurrence.featureValue[Double](f).get * w
    })
  }

  override def toString = "LogLinearFeatureMixture[%s]".format(
    weightedFeatures.map{ case (f, w) => f + " * " + w }.mkString(" ")
  )
}
