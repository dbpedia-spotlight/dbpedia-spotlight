package org.dbpedia.spotlight.db

import io._
import memory.{MemoryTokenStore, MemoryResourceStore, MemoryStore}
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.io.FileOccurrenceSource
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.model.SurfaceForm

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


    memoryIndexer.addSurfaceForms(
      SurfaceFormSource.fromTSVFile(
        new File("raw_data/csv/surfaceForms-fromOccs-thresh10-TRD.set")
      )
    )

    memoryIndexer.addResources(
      DBpediaResourceSource.fromTSVFiles(
        new File("raw_data/csv/conceptURIs.list"),
        new File("raw_data/csv/uri.count.tsv"),
        new File("raw_data/csv/instanceTypes.tsv")
      )
    )

    val sfStore = MemoryStore.loadSurfaceFormStore(new FileInputStream("data/sf.mem"))
    memoryIndexer.addCandidates(
      CandidateMapSource.fromTSVFile(
        new File("raw_data/csv/candidateMap.count"),
        MemoryStore.loadResourceStore(new FileInputStream("data/res.mem")),
        sfStore),
      sfStore.size
    )

    memoryIndexer.addTokens(
      TokenSource.fromOccurrenceSource(
        FileOccurrenceSource.fromFile(new File("raw_data/csv/occs.uriSorted.thresh10.tsv.gz")),
        new LuceneTokenizer(new StandardAnalyzer(Version.LUCENE_36))
      )
    )

    IndexTokenOccurrences.index(
      memoryIndexer,
      FileOccurrenceSource.fromFile(new File("raw_data/csv/occs.uriSorted.thresh10.tsv.gz")),
      MemoryStore.loadTokenStore(new FileInputStream("data/tokens.mem")),
      new LuceneTokenizer(new StandardAnalyzer(Version.LUCENE_36)),
      MemoryStore.loadResourceStore(new FileInputStream("data/res.mem"))
    )


  }

}
