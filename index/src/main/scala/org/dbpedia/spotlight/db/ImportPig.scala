package org.dbpedia.spotlight.db

import io._
import java.io.{FileInputStream, File}
import memory.{MemorySurfaceFormStore, MemoryTokenStore, MemoryResourceStore, MemoryStore}
import org.dbpedia.spotlight.io.FileOccurrenceSource
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.model.SurfaceForm
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object ImportPig {

  def main(args: Array[String]) {

    val memoryIndexer = new MemoryStoreIndexer(new File("data/"))
    //val diskIndexer = new JDBMStoreIndexer(new File("data/"))

    val wikipediaToDBPediaClosure = new WikipediaToDBpediaClosure(
      new FileInputStream(new File("/Volumes/Daten/DBpedia/Spotlight/Pig/redirects_en.nt"))
    )

    memoryIndexer.addResources(
      DBpediaResourceSource.fromPigFiles(
        wikipediaToDBPediaClosure,
        new File("/Volumes/Daten/DBpedia/Spotlight/Pig/nerd_stats_absolute_counts_enwiki_20120601_5grams/uriCounts"),
        new File("/Volumes/Daten/DBpedia/Spotlight/instanceTypes.tsv")
      )
    )

    memoryIndexer.addSurfaceForms(
      SurfaceFormSource.fromPigFile(
        new File("/Volumes/Daten/DBpedia/Spotlight/Pig/nerd_stats_absolute_counts_enwiki_20120601_5grams/sfCounts")
      )
    )


    val sfStore  = MemoryStore.load[MemorySurfaceFormStore](new FileInputStream("data/sf.mem"), new MemorySurfaceFormStore())
    val resStore = MemoryStore.load[MemoryResourceStore](new FileInputStream("data/res.mem"), new MemoryResourceStore())

    memoryIndexer.addCandidates(
      CandidateMapSource.fromPigFiles(
        new File("/Volumes/Daten/DBpedia/Spotlight/Pig/nerd_stats_absolute_counts_enwiki_20120601_5grams/pairCounts"),
        wikipediaToDBPediaClosure,
        resStore,
        sfStore),
      sfStore.size
    )

    memoryIndexer.addTokens(
      TokenSource.fromPigFile(
        new File("/Volumes/Daten/DBpedia/Spotlight/Pig/token_counts_min_2.TSV")
      )
    )

    val tokenStore = MemoryStore.load[MemoryTokenStore](new FileInputStream("data/tokens.mem"), new MemoryTokenStore())
    memoryIndexer.addTokenOccurrences(
      TokenOccurrenceSource.fromPigFile(
        new File("/Volumes/Daten/DBpedia/Spotlight/Pig/token_counts_min_2.TSV"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      )
    )


  }

}
