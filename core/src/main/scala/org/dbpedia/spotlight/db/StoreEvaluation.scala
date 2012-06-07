package org.dbpedia.spotlight.db

import collection.mutable.ListBuffer

import disk.DiskSurfaceFormStore
import memory.MemorySurfaceFormStore
import model.SurfaceFormStore
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

    val store: SurfaceFormStore = new DiskSurfaceFormStore("data/sf.disk")
    //val store: MemoryBasedSurfaceFormStore = MemoryStore$.load[MemoryBasedSurfaceFormStore](new FileInputStream("sf.mem"))

    val t = ListBuffer[Long]()

    var i = 0
    val rs = Seq.fill(10000)(Random.nextInt(1000000)).toSet

    scala.io.Source.fromFile("/Users/jodaiber/Desktop/DBpedia/spots.set").getLines() foreach {
      line: String => {
        if (rs.contains(i)) {
          val b = System.currentTimeMillis
          val form: SurfaceForm = store.getSurfaceForm(line.trim)
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