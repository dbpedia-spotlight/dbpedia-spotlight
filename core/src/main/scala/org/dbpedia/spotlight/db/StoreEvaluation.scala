package org.dbpedia.spotlight.db

import collection.mutable.ListBuffer

import memory._
import model._
import util.Random
import java.io.FileInputStream
import scala.Predef._
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.disambiguate.mixtures.{LinearRegressionMixture, Mixture}
import org.dbpedia.spotlight.spot.WikiMarkupSpotter
import org.dbpedia.spotlight.model._
import scala.collection.JavaConverters._
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.snowball.SnowballAnalyzer

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object StoreEvaluation {

  def main(args: Array[String]) {

    val consumption = ListBuffer[Long]()

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)
    val sfStore = MemoryStore.loadSurfaceFormStore(new FileInputStream("data/sf.mem"))

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)
    val resStore = MemoryStore.loadResourceStore(new FileInputStream("data/res.mem"))

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)

    val cm = MemoryStore.loadCandidateMapStore(new FileInputStream("data/candmap.mem"), resStore)

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)

    val tokenStore = MemoryStore.loadTokenStore(new FileInputStream("data/tokens.mem"))

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)

    val contextStore = MemoryStore.loadContextStore(new FileInputStream("data/context.mem"), tokenStore)

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)
    println("Memory consumption:", consumption)


    val disambiguator = new DBTwoStepDisambiguator(
      tokenStore,
      sfStore,
      resStore,
      cm,
      contextStore,
      new LuceneTokenizer(new SnowballAnalyzer(Version.LUCENE_36, "English")),
      new LinearRegressionMixture()
    )

    val spotter = new WikiMarkupSpotter()
    val t = new Text("[[Berlin]] is the capital of [[Germany]].")
    val spots = spotter.extract(t)
    val p = new Paragraph(t, spots.asScala.toList)

    println(disambiguator.bestK(p, 10))

  }

}