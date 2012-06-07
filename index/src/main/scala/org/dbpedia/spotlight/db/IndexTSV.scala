package org.dbpedia.spotlight.db

import io.{DBpediaResourceSource, SurfaceFormSource}
import java.io.File
import org.dbpedia.spotlight.model.SurfaceForm

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object IndexTSV {

  def main(args: Array[String]) {

    val indexer = new JDBMStoreIndexer(new File("data/"))
    //val indexer = new MemoryStoreIndexer(new File("data/"))

    //indexer.addSurfaceForms(
    //  SurfaceFormSource.fromTSVFile(
    //    new File("/Volumes/Daten/DBpedia/Spotlight/surfaceForms-fromOccs-thresh10-TRD.set")
    //  ).map{ sf: SurfaceForm => (sf, sf.support) }
    //)

    indexer.addResources(
      DBpediaResourceSource.fromTSVFile(
        new File("/Volumes/Daten/DBpedia/Spotlight/conceptURIs.list"),
        new File("/Volumes/Daten/DBpedia/Spotlight/uri.count.tsv"),
        new File("/Volumes/Daten/DBpedia/Spotlight/instanceTypes.tsv")
      )
    )



  }

}
