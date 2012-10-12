package org.dbpedia.spotlight.db

import io._
import java.io.{FileInputStream, File}
import memory.MemoryStore

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


    memoryIndexer.addSurfaceForms(
      SurfaceFormSource.fromPigFiles(
        new File("raw_data/pig/sfCounts"),
        new File("raw_data/pig/phrasesCounts")
      )
    )

    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      new FileInputStream(new File("raw_data/pig/redirects_en.nt")),
      new FileInputStream(new File("raw_data/pig/disambiguations_en.nt"))
    )

    memoryIndexer.addResources(
      DBpediaResourceSource.fromPigFiles(
        wikipediaToDBpediaClosure,
        new File("raw_data/pig/uriCounts"),
        new File("raw_data/pig/instanceTypes.tsv")
      )
    )


    val sfStore  = MemoryStore.loadSurfaceFormStore(new FileInputStream("data/sf.mem"))
    val resStore = MemoryStore.loadResourceStore(new FileInputStream("data/res.mem"))

    memoryIndexer.addCandidatesByID(
      CandidateMapSource.fromPigFiles(
        new File("raw_data/pig/pairCounts"),
        wikipediaToDBpediaClosure,
        resStore,
        sfStore),
      sfStore.size
    )

    memoryIndexer.addTokens(
      TokenSource.fromPigFile(
        new File("raw_data/pig/tokens_json")
      )
    )

    val tokenStore = MemoryStore.loadTokenStore(new FileInputStream("data/tokens.mem"))

    memoryIndexer.createContextStore(resStore.size)
    memoryIndexer.addTokenOccurrences(
      TokenOccurrenceSource.fromPigFile(
        new File("raw_data/pig/tokens_json"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      )
    )
    memoryIndexer.writeTokenOccurrences()


  }

}
