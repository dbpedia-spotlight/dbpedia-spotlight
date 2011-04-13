package org.dbpedia.spotlight.disambiguate.mixtures

/**
 * Linear regression mixture
 * The values below were obtained by Linear Regression over a set of Wikipedia occurrences
 *
 * @author maxjakob
 * @author pablomendes
 */

//TODO everything is hard-coded here :(

class LinearRegressionMixture extends Mixture(0) {


    //344.597 * prior + 1.1247 * score - 0.0055
//    override val contextWeight = 1.1247
//    val priorWeight = 344.597
//    val c = -0.0055

    //232.1878 * prior + 1.5252 * score + 0.2573
    override val contextWeight =  1.5252
    val priorWeight = 232.1878
    val c = 0.2573

    val totalOccurrenceCount = 69772256


    def getScore(contextScore: Double, uriCount: Int) = {
        contextWeight * contextScore + priorWeight * uriCount/totalOccurrenceCount + c
    }

    override def toString = "LinearRegressionMixture[priorWeight="+priorWeight+",c="+c+"]("+contextWeight+")"

}