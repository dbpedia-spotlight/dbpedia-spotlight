package org.dbpedia.spotlight.disambiguate.mixtures

/**
 * Linear regression mixture
 */

//TODO everything is hard-coded here :(

class LinearRegressionMixture extends Mixture(0) {

    override val contextWeight = 1.1247
    val priorWeight = 344.597
    val c = -0.0055

    val totalOccurrenceCount = 69772256


    def getScore(contextScore: Double, uriCount: Int) = {
        contextWeight * contextScore + priorWeight * uriCount/totalOccurrenceCount + c
    }

    override def toString = "LinearRegressionMixture[priorWeight="+priorWeight+",c="+c+"]("+contextWeight+")"

}