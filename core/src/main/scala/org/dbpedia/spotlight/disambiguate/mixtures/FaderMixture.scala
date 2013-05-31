package org.dbpedia.spotlight.disambiguate.mixtures

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

/**
 * Adaptation of Fader et al. (2009) mixture
 */

class FaderMixture(override val contextWeight: Double, val alpha: Double, val surrogatesCount: Int) extends Mixture(contextWeight) {

//    def getScore(contextScore: Double, uriCount: Int) = {
//        val queryMatch = 1  //TODO not implemented yet
//        val prominence = 1 + math.log( 1 + uriCount / alpha )
//        val searchScore = queryMatch * prominence
//        val searchScoreLambda = (contextWeight / surrogatesCount) + (1 - contextWeight) * searchScore
//
//        contextScore * searchScoreLambda
//    }

    def getScore(occurrence: DBpediaResourceOccurrence) : Double = {
        val contextScore = occurrence.contextualScore
        val prior = occurrence.resource.prior

        val queryMatch = 1  //TODO not implemented yet
        val prominence = 1 + math.log( 1 + prior * alpha )
        val searchScore = queryMatch * prominence
        val searchScoreLambda = (contextWeight / surrogatesCount) + (1 - contextWeight) * searchScore

        contextScore * searchScoreLambda
    }


    override def toString = "FaderMixture[alpha="+alpha+",surrogatesCount="+surrogatesCount+"]("+contextWeight+")"

}