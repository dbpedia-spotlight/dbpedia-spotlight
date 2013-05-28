package org.dbpedia.spotlight.disambiguate.mixtures

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

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
    //override val contextWeight =  1.5252
    //val priorWeight = 232.1878
    //val c = 0.2573

    //incorporating web prior
    // 109.8291 * prior + 0.0027 * score + 0.7257
//    override val contextWeight =  0.0027
//    val priorWeight = 109.8291
//    val c = 0.7257


    val priorWeight = 6617.888
    override val contextWeight = 0.7886
    val c = 0.2214


    val totalOccurrenceCount = 69772256


//    def getScore(contextScore: Double, uriCount: Int) = {
//        contextWeight * contextScore + priorWeight * uriCount/totalOccurrenceCount + c
//    }

    def getScore(occurrence: DBpediaResourceOccurrence) : Double = {
        val contextualScore = occurrence.contextualScore
        val prior = occurrence.resource.prior
//        priorWeight * prior + contextWeight * contextualScore + c

// .47 beats prior!
   1234.3989 * prior +
      0.9968 * contextualScore +
     -0.0275

//341.6602 * prior +
//      2.8665 * contextualScore +
//      0.1877
    }


    override def toString = "LinearRegressionMixture[priorWeight="+priorWeight+",c="+c+"]("+contextWeight+")"

}