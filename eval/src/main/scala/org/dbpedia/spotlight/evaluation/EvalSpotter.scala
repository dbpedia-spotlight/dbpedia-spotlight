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
        evalSpotting(annotatedTextSource, new SimpleAnalyzer(Version.LUCENE_36))
        evalSpotting(annotatedTextSource, new StopAnalyzer(Version.LUCENE_36))
        evalSpotting(annotatedTextSource, new ClassicAnalyzer(Version.LUCENE_36))
        evalSpotting(annotatedTextSource, new StandardAnalyzer(Version.LUCENE_36))
        evalSpotting(annotatedTextSource, new EnglishAnalyzer(Version.LUCENE_36))
        evalSpotting(annotatedTextSource, new WhitespaceAnalyzer(Version.LUCENE_36))
    }

    def evalSpotting(annotatedTextSource: AnnotatedTextSource, analyzer: Analyzer) {
        var expected = Set[SurfaceFormOccurrence]()

        // index
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

        var truePositive = 0
        var falseNegative = 0
        for (e <- expected) {
            if (actual contains e) {
                truePositive += 1
                actual = actual - e
            } else {
                falseNegative += 1
                LOG.debug("false negative: " + e)
            }
        }
        val falsePositive = actual.size

        val precision = truePositive.toDouble / (truePositive + falseNegative)
        val recall = truePositive.toDouble / (truePositive + falsePositive)

        LOG.info("Corpus: " + annotatedTextSource.name)
        LOG.info("Spotter: " + lingPipeSpotter.getName)
        LOG.info("Analyzer: " + analyzer.getClass)
        LOG.info("           | actual Y  | actual N")
        LOG.info("expected Y |   %3d     |    %3d".format(truePositive, falseNegative))
        LOG.info("expected N |   %3d     |    N/A".format(falsePositive))
        LOG.info("precision: %f  recall: %f".format(precision, recall))
        LOG.info("--------------------------------")
    }

}
