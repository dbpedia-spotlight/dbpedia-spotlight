package org.dbpedia.spotlight.disambiguate.mixtures

/**
 * Only look at context score
 */

class OnlySimScoreMixture extends Mixture(1) {

    def getScore(contextScore: Double, uriCount: Int) = {
        contextScore
    }

    override def toString = "OnlyContextMixture"

}