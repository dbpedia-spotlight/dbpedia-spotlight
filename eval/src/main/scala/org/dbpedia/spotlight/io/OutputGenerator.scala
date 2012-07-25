package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.{DisambiguationResult, HasFeatures, DBpediaResourceOccurrence}

/**
 *
 * @author pablomendes
 */

trait OutputGenerator {

    def write(result: DisambiguationResult)

    def close()

    def flush()

    def summary(summaryString: String)

}
