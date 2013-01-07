package org.dbpedia.spotlight.disambiguate.mixtures

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

/**
 * A linear regression mixture based on [[org.dbpedia.spotlight.model.Feature]]s.
 *
 * The mixture is instantiated with the name of the feature and its weight as well as the function offset, e.g.:
 *
 * new LinearRegressionFeatureMixture(List(("P(e)", 0.0216), ("P(c|e)", 0.0005), ("P(s|e)", 0.2021)), 1.5097)
 *
 * @author Joachim Daiber
 */

class LinearRegressionFeatureMixture(weightedFeatures: List[Pair[String, Double]], offset: Double) extends Mixture(0) {

  def getScore(occurrence: DBpediaResourceOccurrence) : Double = {

    weightedFeatures.map{ case (f, w) =>
      occurrence.featureValue[Double](f).get * w
    }.sum + offset

  }

  override def toString = "LinearRegressionFeatureMixture[%s]".format(
    weightedFeatures.map{ case (f, w) => f + " * " + w }.mkString(" ") + " + " + offset
  )
}