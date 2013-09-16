package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.log.SpotlightLog

/**
 *
 * @author pablomendes
 */

class DisambiguationResult(val correctOccurrence: DBpediaResourceOccurrence, val predictedOccurrences: List[DBpediaResourceOccurrence] ) {

    lazy val rank = {
        SpotlightLog.debug(this.getClass, "Ranking for: %s -> %s", correctOccurrence.surfaceForm,correctOccurrence.resource)
        SpotlightLog.debug(this.getClass, "K=%s", predictedOccurrences.size)
        var rank,i = 0
        //SpotlightLog.debug(this.getClass, "                : prior \t context \t final \t uri")
        SpotlightLog.debug(this.getClass, "                : context \t uri")
        for(predictedOccurrence <- predictedOccurrences) {
            i = i + 1
            if(correctOccurrence.resource equals predictedOccurrence.resource) {
                rank = i
                //SpotlightLog.debug(this.getClass, "  **     correct: %.5f \t %.5f \t %.5f \t %s", predictedOccurrence.resource.prior, predictedOccurrence.contextualScore, predictedOccurrence.similarityScore, predictedOccurrence.resource)
                SpotlightLog.debug(this.getClass, "  **     correct: %.5f \t %s", predictedOccurrence.contextualScore, predictedOccurrence.resource)
            }
            else {
                //SpotlightLog.debug(this.getClass, "       spotlight: %.5f \t %.5f \t %.5f \t %s", predictedOccurrence.resource.prior, predictedOccurrence.contextualScore, predictedOccurrence.similarityScore, predictedOccurrence.resource)
                SpotlightLog.debug(this.getClass, "       spotlight: %.5f \t %s", predictedOccurrence.contextualScore, predictedOccurrence.resource)
            }
        }
        if (rank==0)
            SpotlightLog.debug(this.getClass, "  **   not found: %.5s \t %.5s \t %.5s \t %s", "NA", "NA", "NA", correctOccurrence.resource)
        SpotlightLog.debug(this.getClass, "Rank: %s", rank)
        rank
    }

    lazy val predicted = predictedOccurrences.headOption

}
