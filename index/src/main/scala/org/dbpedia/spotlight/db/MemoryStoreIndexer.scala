package org.dbpedia.spotlight.db

import gnu.trove.TObjectIntHashMap
import memory.MemoryBasedStores.MemoryBasedSurfaceFormStore
import memory.MemoryBasedStores
import io.Source
import java.io.{FileInputStream, File, InputStream}
import collection.mutable.ListBuffer

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object MemoryStoreIndexer {

  def addSurfaceForms(
    input: InputStream
    ): MemoryBasedSurfaceFormStore = {
    var i = 1

    val supportForID = ListBuffer[Int]()
    supportForID += 0
    val idForString = new TObjectIntHashMap()


    Source.fromInputStream(input) getLines() foreach {
      line: String => {
        val name = line.trim()

        idForString.put(name, i)
        supportForID += 1

        i += 1
      }
    }

    val store: MemoryBasedSurfaceFormStore = new MemoryBasedSurfaceFormStore()
    store.idForString = idForString
    store.supportForID = supportForID.toArray
    store
  }


  def main(args: Array[String]) {

    MemoryBasedStores.save[MemoryBasedSurfaceFormStore](
      addSurfaceForms(new FileInputStream(new File("/Volumes/Daten/DBpedia/Spotlight/surfaceForms-fromOccs-thresh10-TRD.set"))),
      new File("sf.mem")
    )

  }

}