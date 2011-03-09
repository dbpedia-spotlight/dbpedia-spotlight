package org.dbpedia.spotlight.disambiguate.mixtures

/**
 * Adaptation of Fader et al. (2009) mixture
 */

class LinearRegressionMixture(override val contextWeight: Double, val priorWeight: Double) extends Mixture(contextWeight) {

    def getScore(contextScore: Double, uriCount: Int) = {
        contextWeight * contextScore + priorWeight * uriCount
    }

    override def toString = "LinearRegressionMixture[priorWeight="+priorWeight+"]("+contextWeight+")"

}