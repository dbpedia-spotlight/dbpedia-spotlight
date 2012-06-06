package org.dbpedia.spotlight.db

import disk.DiskBasedStores.DiskBasedSurfaceFormStore
import io.Source
import memory.MemoryBasedStores
import memory.MemoryBasedStores.MemoryBasedSurfaceFormStore
import collection.mutable.ListBuffer
import model.SurfaceFormStore
import tools.nsc.io.File
import java.io.FileInputStream
import util.Random
import org.dbpedia.spotlight.model.SurfaceForm

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object StoreEvaluation {

  def main(args: Array[String]) {

    val store: SurfaceFormStore = new DiskBasedSurfaceFormStore("sf")
    //val store: MemoryBasedSurfaceFormStore = MemoryBasedStores.load[MemoryBasedSurfaceFormStore](new FileInputStream("sf.mem"))

    val t = ListBuffer[Long]()

    var i = 0
    val rs = Seq.fill(10000)(Random.nextInt(1000000)).toSet

    Source.fromFile("/Users/jodaiber/Desktop/DBpedia/spots.set").getLines() foreach {
      line: String => {
        if (rs.contains(i)) {
          val b = System.currentTimeMillis
          val form: SurfaceForm = store.get(line.trim)
          println(form)
          t.append(System.currentTimeMillis - b)
          println(t.last)
        }
        i+=1
      }
    }

    println("Avg: " + t.reduceLeft[Long](_ + _) / t.size.toFloat)

  }

}