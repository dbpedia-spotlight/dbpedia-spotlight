package org.dbpedia.spotlight.db

import collection.mutable.ListBuffer

import disk.DiskSurfaceFormStore
import memory._
import model.SurfaceFormStore
import util.Random
import java.io.FileInputStream
import org.dbpedia.spotlight.model.{Candidate, DBpediaResource, SurfaceForm}

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

   // consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)
   // val cm = MemoryStore.load[MemoryCandidateMapStore](new FileInputStream("data/candmap.mem"), new MemoryCandidateMapStore())
   // consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)

    val tokenStore = MemoryStore.load[MemoryTokenStore](new FileInputStream("data/tokens.mem"), new MemoryTokenStore())
    consumption += (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024 * 1024)



    println(consumption)

    //cm.resourceStore = resStore

    val name: DBpediaResource = resStore.getResourceByName("Germany")
    println(sfStore.idForString.size())


    //val candidates = cm.getCandidates(sfStore.getSurfaceForm("Germany")).toList.sortBy(_.support).reverse
    //println(cm.size)
    //println(candidates)


    System.gc(); System.gc(); System.gc(); System.gc(); System.gc(); System.gc();
    System.gc(); System.gc(); System.gc(); System.gc(); System.gc(); System.gc();

    println(1)
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