package org.dbpedia.spotlight.db

import io._
import memory.{MemoryTokenStore, MemoryResourceStore, MemoryStore}
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.io.FileOccurrenceSource
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object ImportTSV {

  def main(args: Array[String]) {

    val memoryIndexer = new MemoryStoreIndexer(new File("data/"))
    val diskIndexer = new JDBMStoreIndexer(new File("data/"))


    //memoryIndexer.addSurfaceForms(
    //  SurfaceFormSource.fromTSVFile(
    //    new File("/Volumes/Daten/DBpedia/Spotlight/surfaceForms-fromOccs-thresh10-TRD.set")
    //  ).map{ sf: SurfaceForm => (sf, sf.support) }
    //)

    //memoryIndexer.addResources(
    //  DBpediaResourceSource.fromTSVFile(
    //    new File("/Users/jodaiber/Desktop/conceptURIs.list"),
    //    new File("/Users/jodaiber/Desktop/uri.count.tsv"),
    //    new File("/Users/jodaiber/Desktop/instanceTypes.tsv")
    //  )
    //)

   // val sfStore = MemoryStore.load[MemorySurfaceFormStore](new FileInputStream("data/sf.mem"), new MemorySurfaceFormStore())
   // memoryIndexer.addCandidates(
   //   CandidateMapSource.fromTSVFile(
   //     new File("/Volumes/Daten/DBpedia/Spotlight/candidateMap.count"),
   //     MemoryStore.load[MemoryResourceStore](new FileInputStream("data/res.mem"), new MemoryResourceStore()),
   //     sfStore),
   //   sfStore.size
   // )

    //memoryIndexer.addTokens(
    //  TokenSource.fromOccurrenceSource(
    //    FileOccurrenceSource.fromFile(new File("/Volumes/Daten/DBpedia/Spotlight/occs.uriSorted.thresh10.tsv.gz")),
    //    new LuceneTokenizer(new StandardAnalyzer(Version.LUCENE_36))
    //  )
    //)

    IndexTokenOccurrences.index(
      memoryIndexer,
      FileOccurrenceSource.fromFile(new File("/Volumes/Daten/DBpedia/Spotlight/occs.uriSorted.thresh10.tsv.gz")),
      MemoryStore.load[MemoryTokenStore](new FileInputStream("data/tokens.mem"), new MemoryTokenStore()),
      new LuceneTokenizer(new StandardAnalyzer(Version.LUCENE_36)),
      MemoryStore.load[MemoryResourceStore](new FileInputStream("data/res.mem"), new MemoryResourceStore())
    )


  }

}
