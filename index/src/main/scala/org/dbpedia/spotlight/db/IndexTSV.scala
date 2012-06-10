package org.dbpedia.spotlight.db

import io.{CandidateMapSource, SurfaceFormSource, DBpediaResourceSource}
import memory.{MemoryResourceStore, MemorySurfaceFormStore, MemoryStore}
import org.dbpedia.spotlight.model.SurfaceForm
import java.io.{FileInputStream, File}

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

    indexer.addResources(
      DBpediaResourceSource.fromTSVFile(
        new File("/Users/jodaiber/Desktop/conceptURIs.list"),
        new File("/Users/jodaiber/Desktop/uri.count.tsv"),
        new File("/Users/jodaiber/Desktop/instanceTypes.tsv")
      )
    )

    val sfStore = MemoryStore.load[MemorySurfaceFormStore](new FileInputStream("data/sf.mem"), new MemorySurfaceFormStore())
    indexer.addCandidates(
      CandidateMapSource.fromTSVFile(
        new File("/Users/jodaiber/Desktop/candidateMap.count"),
        MemoryStore.load[MemoryResourceStore](new FileInputStream("data/res.mem"), new MemoryResourceStore()),
        sfStore),
      sfStore.size
    )

  }

}
