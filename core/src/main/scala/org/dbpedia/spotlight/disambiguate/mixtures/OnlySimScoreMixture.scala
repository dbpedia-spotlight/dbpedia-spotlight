package org.dbpedia.spotlight.disambiguate.mixtures

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

/**
 * Only look at context score
 */

class OnlySimScoreMixture extends Mixture(1) {

    def getScore(occurrence: DBpediaResourceOccurrence) : Double = {
        occurrence.contextualScore
    }

    override def toString = "OnlyContextMixture"

}