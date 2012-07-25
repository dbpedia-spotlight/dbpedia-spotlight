package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.io.AnnotatedTextSource
import java.io.File

/**
 * This class iterates over a corpus of DBpediaResourceOccurrence or AnnotatedText
 * and generates statistics about the dataset.
 *
 * For each correct annotation:
 * - support
 * - contextual score
 * - percentage of second
 * - type
 * - category?
 * - ambiguity of surface form
 *
 * For each text:
 * - number of tokens
 *
 * @author pablomendes
 */

class GenerateDatasetStatistics {

    def main(args: Array[String]) {
        val testFileName = if (args.size>0) args(0) else "/home/pablo/eval/gold/CSAWoccs.sortedpars.tsv"

        val paragraphs = AnnotatedTextSource.fromOccurrencesFile(new File(testFileName))

    }

}