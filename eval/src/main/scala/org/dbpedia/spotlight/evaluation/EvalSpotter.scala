package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.corpus.{CSAWCorpus, MilneWittenCorpus}
import java.io.File
import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import com.aliasi.dict.{DictionaryEntry, MapDictionary}
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.model.{Factory, SurfaceFormOccurrence}
import collection.JavaConversions
import org.apache.lucene.analysis._
import org.apache.commons.logging.LogFactory
import org.apache.lucene.analysis.standard.{StandardAnalyzer, ClassicAnalyzer}

/**
 *
 *
 */
object EvalSpotter {

    private val LOG = LogFactory.getLog(this.getClass)

    def main(args: Array[String]) {
        evalSpotting(MilneWittenCorpus.fromDirectory(new File("/home/max/spotlight-data/milne-witten")))
        //evalSpotting(AnnotatedTextSource.fromOccurrencesFile(new File("/home/max/spotlight-data/CSAWoccs.red-dis-3.7-sorted.tsv")))
    }

    def evalSpotting(annotatedTextSource: AnnotatedTextSource) {
        val analyzers = List(
            new SimpleAnalyzer(Version.LUCENE_36),
            new StopAnalyzer(Version.LUCENE_36),
            new ClassicAnalyzer(Version.LUCENE_36),
            new StandardAnalyzer(Version.LUCENE_36),
            new EnglishAnalyzer(Version.LUCENE_36),
            new WhitespaceAnalyzer(Version.LUCENE_36)
        )
        for (analyzer <- analyzers) {
            evalSpotting(annotatedTextSource, analyzer)
        }
    }

    def evalSpotting(annotatedTextSource: AnnotatedTextSource, analyzer: Analyzer) {
        // create gold standard and index
        var expected = Set[SurfaceFormOccurrence]()
        val dictionary = new MapDictionary[String]()
        for (paragraph <- annotatedTextSource;
             occ <- paragraph.occurrences) {
            expected += Factory.SurfaceFormOccurrence.from(occ)
            dictionary.addEntry(new DictionaryEntry[String](occ.surfaceForm.name, ""))
        }
        val lingPipeSpotter: Spotter = new LingPipeSpotter(dictionary, analyzer)

        // eval
        var actual = Set[SurfaceFormOccurrence]()
        for (paragraph <- annotatedTextSource) {
            actual = JavaConversions.asScalaBuffer(lingPipeSpotter.extract(paragraph.text)).toSet union actual
        }

        printResults("LingPipeSpotter with %s and corpus %s".format(analyzer.getClass, annotatedTextSource.name),
            expected, actual)
    }

    def printResults(description: String, expected: Set[SurfaceFormOccurrence], actual: Set[SurfaceFormOccurrence]) {
        var truePositive = 0
        var falseNegative = 0
        for (e <- expected) {
            if (actual contains e) {
                truePositive += 1
            } else {
                falseNegative += 1
                LOG.debug("false negative: " + e)
            }
        }
        val falsePositive = actual.size - truePositive

        val precision = truePositive.toDouble / (truePositive + falseNegative)
        val recall = truePositive.toDouble / (truePositive + falsePositive)

        LOG.info(description)
        LOG.info("           | actual Y  | actual N")
        LOG.info("expected Y |   %3d     |    %3d".format(truePositive, falseNegative))
        LOG.info("expected N |   %3d     |    N/A".format(falsePositive))
        LOG.info("precision: %f  recall: %f".format(precision, recall))
        LOG.info("--------------------------------")
    }

}
