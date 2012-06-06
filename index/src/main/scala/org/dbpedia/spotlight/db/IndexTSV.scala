package org.dbpedia.spotlight.db

import java.io.File
import org.dbpedia.spotlight.io.SurfaceFormSource
import org.dbpedia.spotlight.model.SurfaceForm

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object IndexTSV {

  def main(args: Array[String]) {

    //val indexer = new JDBMStoreIndexer(new File("data/"))
    val indexer = new MemoryStoreIndexer(new File("data/"))

    indexer.addSurfaceForms(
      SurfaceFormSource.fromTSVFile(
        new File("/Volumes/Daten/DBpedia/Spotlight/surfaceForms-fromOccs-thresh10-TRD.set")
      ).map{ sf: SurfaceForm => (sf, sf.support) }
    )


  }

}
