package org.dbpedia.spotlight.disambiguate.mixtures

/**
 * Adaptation of Fader et al. (2009) mixture
 */

class Fader2Mixture(override val contextWeight: Double, val alpha: Double) extends Mixture(contextWeight) {

    def getScore(contextScore: Double, uriCount: Int) = {
        val prominence = 1 + math.log( 1 + uriCount / alpha )

        (contextWeight * contextScore) + (1 - contextWeight) * prominence
    }

    override def toString = "Fader2Mixture[alpha="+alpha+"]("+contextWeight+")"

}