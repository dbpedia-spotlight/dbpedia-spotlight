package org.dbpedia.spotlight.db

import collection.mutable.ListBuffer

import disk.{DiskContextStore, DiskSurfaceFormStore}
import memory._
import model._
import util.Random
import java.io.FileInputStream
import scala.Predef._
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.disambiguate.mixtures.{LinearRegressionMixture, Mixture}
import org.dbpedia.spotlight.model.{Paragraph, Candidate, DBpediaResource, SurfaceForm}

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
    val sfStore = MemoryStore.load[MemorySurfaceFormStore](new FileInputStream("data/sf.mem"), new MemorySurfaceFormStore())

    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)
    val resStore = MemoryStore.load[MemoryResourceStore](new FileInputStream("data/res.mem"), new MemoryResourceStore())

   consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)
   val cm = MemoryStore.load[MemoryContextStore](new FileInputStream("data/candmap.mem"), new MemoryContextStore())
   consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)

    val tokenStore = MemoryStore.load[MemoryTokenStore](new FileInputStream("data/tokens.mem"), new MemoryTokenStore())
    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)



    println(consumption)


    val contextStore = new DiskContextStore("data/context.disk")

    var b = System.currentTimeMillis
    println(contextStore.getContextCounts(resStore.getResourceByName("Berlin")))
    println("Time:" + (System.currentTimeMillis - b))

    b = System.currentTimeMillis
    println(contextStore.getContextCounts(resStore.getResourceByName("Germany")))
    println("Time:" + (System.currentTimeMillis - b))

    b = System.currentTimeMillis
    println(contextStore.getContextCounts(resStore.getResourceByName("Prague")))
    println("Time:" + (System.currentTimeMillis - b))

    b = System.currentTimeMillis
    println(contextStore.getContextCounts(resStore.getResourceByName("Zoolander")))
    println("Time:" + (System.currentTimeMillis - b))

    b = System.currentTimeMillis
    println(contextStore.getContextCounts(resStore.getResourceByName("Ben_Stiller")))
    println("Time:" + (System.currentTimeMillis - b))

    b = System.currentTimeMillis
    println(contextStore.getContextCounts(resStore.getResourceByName("Larry_Block")).size())
    println("Time:" + (System.currentTimeMillis - b))

   //val disambiguator = new DBTwoStepDisambiguator(
   //  tokenStore,
   //  sfStore,
   //  resStore,
   //  cm,
   //  contextStore,
   //  new LuceneTokenizer(new StandardAnalyzer(Version.LUCENE_36)),
   //  new LinearRegressionMixture()
   //)

    //val candidates = cm.getCandidates(sfStore.getSurfaceForm("Germany")).toList.sortBy(_.support).reverse
    //println(cm.size)
    //println(candidates)


    //val t = ListBuffer[Long]()
//
    //var i = 0
    //val rs = Seq.fill(10000)(Random.nextInt(1000000)).toSet
//
    //scala.io.Source.fromFile("/Users/jodaiber/Desktop/DBpedia/spots.set").getLines() foreach {
    //  line: String => {
    //    if (rs.contains(i)) {
    //      val b = System.currentTimeMillis
    //      val form: SurfaceForm = store.getSurfaceForm(line.trim)
    //      println(form)
    //      t.append(System.currentTimeMillis - b)
    //      println(t.last)
    //    }
    //    i+=1
    //  }
    //}
//
    //println("Avg: " + t.reduceLeft[Long](_ + _) / t.size.toFloat)

  }

}