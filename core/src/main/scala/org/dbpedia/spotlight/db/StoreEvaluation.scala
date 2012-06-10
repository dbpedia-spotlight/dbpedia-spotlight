package org.dbpedia.spotlight.db

import collection.mutable.ListBuffer

import disk.DiskSurfaceFormStore
import memory.{MemoryCandidateMapStore, MemoryResourceStore, MemoryStore, MemorySurfaceFormStore}
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

    val sfStore = MemoryStore.load[MemorySurfaceFormStore](new FileInputStream("data/sf.mem"), new MemorySurfaceFormStore())
    val resStore = MemoryStore.load[MemoryResourceStore](new FileInputStream("data/res.mem"), new MemoryResourceStore())
    val cm = MemoryStore.load[MemoryCandidateMapStore](new FileInputStream("data/candmap.mem"), new MemoryCandidateMapStore())
    cm.resourceStore = resStore

    val name: DBpediaResource = resStore.getResourceByName("Germany")
    println(sfStore.idForString.size())


    val candidates = cm.getCandidates(sfStore.getSurfaceForm("Germany")).toList.sortBy(_.support).reverse
    println(cm.size)
    println(candidates)

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