package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.corpus.{CSAWCorpus, MilneWittenCorpus}
import java.io.File
import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import com.aliasi.dict.{DictionaryEntry, MapDictionary}
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.model.{SurfaceForm, Factory, SurfaceFormOccurrence}
import collection.JavaConversions
import org.apache.lucene.analysis._
import org.dbpedia.spotlight.log.SpotlightLog
import org.apache.lucene.analysis.standard.{StandardAnalyzer, ClassicAnalyzer}
import org.dbpedia.spotlight.spot.ahocorasick.AhoCorasickSpotter

/**
 * This class evaluates spotters by taking an annotated corpus, indexing its surface forms,
 * and checking how many surface forms are found by a number spotters.
 *
 * Set the corpus in evalCorpus.
 * Set the spotters in spotterMethods.
 */
object EvalSpotter {

  def evalCorpus = {
    MilneWittenCorpus.fromDirectory(new File("/home/max/spotlight-data/milne-witten"))
    //AnnotatedTextSource.fromOccurrencesFile(new File("/home/max/spotlight-data/CSAWoccs.red-dis-3.7-sorted.tsv")))
  }

  def spotterMethods: List[Traversable[SurfaceForm] => Spotter] = {
    getLingPipeSpotters :::
      getAhoCorasickSpotter
  }

  def main(args: Array[String]) {
    val expected = getExpectedResult(evalCorpus)
    for (spotter <- spotterMethods) {
      evalSpotting(evalCorpus, spotter, expected)
    }
  }


  private def getAhoCorasickSpotter: List[Traversable[SurfaceForm] => Spotter] = {
    List({
      sfs: Traversable[SurfaceForm] =>
        AhoCorasickSpotter.fromSurfaceForms(sfs.map(_.name), caseSensitive = false, overlap = false)
    })
  }

  private def getLingPipeSpotters: List[Traversable[SurfaceForm] => Spotter] = {
    // LingPipe with different analyzers
    val analyzers = List(
      new SimpleAnalyzer(Version.LUCENE_36),
      new StopAnalyzer(Version.LUCENE_36),
      new ClassicAnalyzer(Version.LUCENE_36),
      new StandardAnalyzer(Version.LUCENE_36),
      new EnglishAnalyzer(Version.LUCENE_36),
      new WhitespaceAnalyzer(Version.LUCENE_36)
    )
    analyzers.map(analyzer => {
      val analyzerName = analyzer.getClass.toString.replaceFirst("^.*\\.", "")
      surfaceForms: Traversable[SurfaceForm] => {
        val dictionary = new MapDictionary[String]()
        for (surfaceForm <- surfaceForms) {
          dictionary.addEntry(new DictionaryEntry[String](surfaceForm.name, ""))
        }
        val lps = new LingPipeSpotter(dictionary, analyzer, false, false)
        lps.setName("LingPipeSpotter[analyzer=%s]".format(analyzerName))
        lps
      }
    })
  }

  def getExpectedResult(annotatedTextSource: AnnotatedTextSource) = {
    annotatedTextSource.foldLeft(Set[SurfaceFormOccurrence]()){ (set, par) =>
      set ++ par.occurrences.map(Factory.SurfaceFormOccurrence.from(_))
    }
  }

  def evalSpotter(annotatedTextSource: AnnotatedTextSource,
                  spotter: Spotter,
                  expected: Traversable[SurfaceFormOccurrence]) {

    // run spotting
    var actual = Set[SurfaceFormOccurrence]()
    for (paragraph <- annotatedTextSource) {
      actual = JavaConversions.asScalaBuffer(spotter.extract(paragraph.text)).toSet union actual
    }

    // compare
    printResults("%s and corpus %s".format(spotter.getName, annotatedTextSource.name), expected, actual)
  }


  private def evalSpotting(annotatedTextSource: AnnotatedTextSource,
                           indexSpotter: Traversable[SurfaceForm] => Spotter,
                           expected: Traversable[SurfaceFormOccurrence]) {
    // index spotter
    val spotter = indexSpotter(expected.map(_.surfaceForm))

    // run spotting
    var actual = Set[SurfaceFormOccurrence]()
    for (paragraph <- annotatedTextSource) {
      actual = JavaConversions.asScalaBuffer(spotter.extract(paragraph.text)).toSet union actual
    }

    // compare
    printResults("%s and corpus %s".format(spotter.getName, annotatedTextSource.name), expected, actual)
  }

  private def printResults(description: String, expected: Traversable[SurfaceFormOccurrence], actual: Set[SurfaceFormOccurrence]) {
    var truePositive = 0
    var falseNegative = 0
    for (e <- expected) {
      if (actual contains e) {
        truePositive += 1
      } else {
        falseNegative += 1
        SpotlightLog.debug(this.getClass, "false negative: %s", e)
      }
    }
    val falsePositive = actual.size - truePositive

    val precision = truePositive.toDouble / (truePositive + falsePositive )
    val recall = truePositive.toDouble / (truePositive + falseNegative)

    SpotlightLog.info(this.getClass, description)
    SpotlightLog.info(this.getClass, "           | actual Y  | actual N")
    SpotlightLog.info(this.getClass, "expected Y |   %3d     |    %3d", truePositive, falseNegative)
    SpotlightLog.info(this.getClass, "expected N |   %3d     |    N/A", falsePositive)
    SpotlightLog.info(this.getClass, "precision: %f  recall: %f", precision, recall)
    SpotlightLog.info(this.getClass, "--------------------------------")
  }

}