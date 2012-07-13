package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.DisambiguationResult
import java.io.PrintWriter

/**
 *
 * @author pablomendes
 */

class TSVOutputGenerator(val output: PrintWriter) extends OutputGenerator {

    var firstLine = true

    def summary(summaryString: String) {
        println(summaryString)
    }

    def flush() {
        output.flush()
    }

    def header(fields: List[String]) = {
        output.append(line(fields))
        firstLine = false
    }

    def line(fields: List[String]) = "\""+fields.mkString("\",\"")+"\"\n"

    def write(result: DisambiguationResult) {

        if (firstLine)
            header(List("occId","correct","surfaceForm","rank","ambiguity"))

        val rank = result.rank.toString
        val ambiguity = result.predictedOccurrences.size.toString
        val fields = List(result.correctOccurrence.id,
                        result.correctOccurrence.resource.uri,
                        result.correctOccurrence.surfaceForm.name,
                        rank,
                        ambiguity)
        output.append(line(fields))
    }

    def close() {
        output.close()
    }
}
