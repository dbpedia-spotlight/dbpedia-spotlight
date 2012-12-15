package org.dbpedia.spotlight.db

import collection.mutable.ListBuffer

import disk.DiskStore
import memory._
import similarity.TFICFSimilarity
import java.io.{File, FileInputStream}
import scala.Predef._
import org.dbpedia.spotlight.disambiguate.mixtures.LinearRegressionMixture
import org.dbpedia.spotlight.spot.WikiMarkupSpotter
import org.dbpedia.spotlight.model._
import scala.collection.JavaConverters._

/**
 * @author Joachim Daiber
 */

object StoreEvaluation {

  def main(args: Array[String]) {

    val consumption = ListBuffer[Long]()

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)
    val sfStore = MemoryStore.loadSurfaceFormStore(new FileInputStream("/bigdrive/4store/index-pig/data/sf.mem"))

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)
    val resStore = MemoryStore.loadResourceStore(new FileInputStream("/bigdrive/4store/index-pig/data/res.mem"))

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)

    val candidateMapStore = MemoryStore.loadCandidateMapStore(new FileInputStream("/bigdrive/4store/index-pig/data/candmap.mem"), resStore)

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)

    val tokenStore = MemoryStore.loadTokenTypeStore(new FileInputStream("/bigdrive/4store/index-pig/data/tokens.mem"))

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)

    //val contextStore = MemoryStore.loadContextStore(new FileInputStream("/bigdrive/4store/index-pig/data/context.mem"), tokenStore)
    val contextStore = DiskStore.loadContextStore(new File("/bigdrive/4store/index-pig/data/context.disk"), tokenStore)

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)
    println("Memory consumption:", consumption)

    val searcher = new DBCandidateSearcher(resStore, sfStore, candidateMapStore)

    val disambiguator = new DBTwoStepDisambiguator(
      tokenStore,
      sfStore,
      resStore,
      searcher,
      contextStore,
      new LinearRegressionMixture(),
      new TFICFSimilarity()
    )

    println(sfStore.getSurfaceFormNormalized("Saint Laurent du Var"))
    println(contextStore.getContextCounts(resStore.getResourceByName("Cannabis_%28drug%29")))
    println(contextStore.getContextCounts(resStore.getResourceByName("Cannabis_(drug)")))

    println(contextStore.getTotalTokenCount(searcher.getCandidates(sfStore.getSurfaceForm("marijuana")).head.resource))

    val spotter = new WikiMarkupSpotter()
    val t = new Text("[[Berlin]] is the capital of [[Germany]].")
    val spots = spotter.extract(t)
    val p = new Paragraph(t, spots.asScala.toList)

    println(disambiguator.bestK(p, 10))

  }

}