package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.{Feature, DisambiguationResult, DBpediaResourceOccurrence}
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
        output.append(line(fields ::: List("class")))
        firstLine = false
    }

    def line(fields: List[String]) = "\""+fields.mkString("\",\"")+"\"\n"

    def write(result: DisambiguationResult) {

        if (firstLine)
            header(List("occId","correct","rank","ambiguity"))

        val rank = result.rank
        val ambiguity = result.predictedOccurrences.size
        output.append("\""+List(result.correctOccurrence.id,
                                                result.correctOccurrence.resource.uri,
                                                rank,
                                                ambiguity).mkString("\"\t\"")+"\"\n")
    }

    def close() {
        output.close()
    }
}
