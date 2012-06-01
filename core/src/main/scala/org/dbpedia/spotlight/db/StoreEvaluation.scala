package org.dbpedia.spotlight.db

import disk.DiskBasedStores.DiskBasedSurfaceFormStore
import io.Source
import util.Random
import org.dbpedia.spotlight.db.model.SurfaceFormStore
import collection.mutable.ListBuffer

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object StoreEvaluation {

  def main(args: Array[String]) {

    val store: SurfaceFormStore = new DiskBasedSurfaceFormStore("sf")

    var lines = Source.fromFile("/Users/jodaiber/Desktop/DBpedia/spots.set").getLines().toList
    var r = Random.shuffle(lines)
    val randomSFs = r.take(10000)
    r = null;
    lines = null

    System.gc();
    System.gc();
    System.gc();
    System.gc();
    System.gc();
    System.gc();
    System.gc();
    System.gc();


    val t = ListBuffer[Long]()
    randomSFs foreach {
      line: String => {
        val b = System.currentTimeMillis
        println(store.get(line.trim))
        t.append(System.currentTimeMillis - b)
        println(t.last)
      }
    }

    println("Avg: " + t.reduceLeft[Long](_ + _) / t.size.toFloat)

  }

}