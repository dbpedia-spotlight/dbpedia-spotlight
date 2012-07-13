package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.{DisambiguationResult, DBpediaResourceOccurrence}
import java.io.PrintWriter

/**
 *
 * @author pablomendes
 */

class TSVOutputGenerator(val output: PrintWriter) extends OutputGenerator {

    def summary(summaryString: String) {
        println(summaryString)
    }

    def flush() {
        output.flush()
    }

    def write(result: DisambiguationResult) {
        val rank = result.rank
        val ambiguity = result.predictedOccurrences.size
        output.append("%s\t%s\t%d\t%d\n".format(result.correctOccurrence.id, result.correctOccurrence.resource.uri, rank, ambiguity))
    }

    def close() {
        output.close()
    }
}
