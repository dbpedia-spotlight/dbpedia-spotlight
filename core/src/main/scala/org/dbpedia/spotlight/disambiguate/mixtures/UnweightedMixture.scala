package org.dbpedia.spotlight.disambiguate.mixtures

import org.dbpedia.spotlight.model.{Feature, DBpediaResourceOccurrence}
import breeze.numerics._

/**
 * Multiplication of scores/probabilities. Assumes probabilities are logarithms.
 *
 * @author Joachim Daiber
 */

class UnweightedMixture(features: Set[String]) extends Mixture(1) {

  def getScore(occurrence: DBpediaResourceOccurrence): Double = {
    val fs = occurrence.features.values.filter({ f: Feature => features.contains(f.featureName) }).map(_.value.asInstanceOf[Double])
    fs.foldLeft(0.0)((x: Double, y: Double) => if(x == -inf || y == -inf) -inf else x + y)
  }

  override def toString = "UnweightedMixture[%s]".format(features.mkString(","))

}