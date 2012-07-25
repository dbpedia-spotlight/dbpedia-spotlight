package org.dbpedia.spotlight.model

import org.apache.commons.logging.LogFactory

/**
 *
 * @author pablomendes
 */

class DisambiguationResult(val correctOccurrence: DBpediaResourceOccurrence, val predictedOccurrences: List[DBpediaResourceOccurrence] ) {

    private val LOG = LogFactory.getLog(this.getClass)

    lazy val rank = {
        LOG.debug("Ranking for: %s -> %s".format(correctOccurrence.surfaceForm,correctOccurrence.resource))
        LOG.debug("K=%s".format(predictedOccurrences.size));
        var rank,i = 0
        //LOG.debug("                : prior \t context \t final \t uri")
        LOG.debug("                : context \t uri")
        for(predictedOccurrence <- predictedOccurrences) {
            i = i + 1
            if(correctOccurrence.resource equals predictedOccurrence.resource) {
                rank = i
                //LOG.debug("  **     correct: %.5f \t %.5f \t %.5f \t %s".format(predictedOccurrence.resource.prior, predictedOccurrence.contextualScore, predictedOccurrence.similarityScore, predictedOccurrence.resource))
                LOG.debug("  **     correct: %.5f \t %s".format(predictedOccurrence.contextualScore, predictedOccurrence.resource))
            }
            else {
                //LOG.debug("       spotlight: %.5f \t %.5f \t %.5f \t %s".format(predictedOccurrence.resource.prior, predictedOccurrence.contextualScore, predictedOccurrence.similarityScore, predictedOccurrence.resource))
                LOG.debug("       spotlight: %.5f \t %s".format(predictedOccurrence.contextualScore, predictedOccurrence.resource))
            }
        }
        if (rank==0)
            LOG.debug("  **   not found: %.5s \t %.5s \t %.5s \t %s".format("NA", "NA", "NA", correctOccurrence.resource))
        LOG.debug("Rank: %s".format(rank))
        rank
    }

    lazy val predicted = predictedOccurrences.headOption

}
