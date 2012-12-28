package org.dbpedia.spotlight.db

import io._
import java.io.{FileInputStream, File}
import memory.MemoryStore
import model.ResourceStore

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object ImportPig {

  def main(args: Array[String]) {
    val rawDataFolder = new File("/data/spotlight/processed/")
    val modelDataFolder = new File("/data/spotlight/models/")

    val memoryIndexer = new MemoryStoreIndexer(modelDataFolder)
    //val diskIndexer = new JDBMStoreIndexer(new File("data/"))

    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      new FileInputStream(new File(rawDataFolder, "pig/redirects.nt")),
      new FileInputStream(new File(rawDataFolder, "pig/disambiguations.nt"))
    )

    memoryIndexer.addResources(
      DBpediaResourceSource.fromPigFiles(
        wikipediaToDBpediaClosure,
        new File(rawDataFolder, "pig/uriCounts"),
        null //new File("raw_data/pig/instanceTypes.tsv")
      )
    )

    val resStore = MemoryStore.loadResourceStore(new FileInputStream(new File(modelDataFolder, "res.mem")))

    memoryIndexer.addSurfaceForms(
      SurfaceFormSource.fromPigFiles(
        new File(rawDataFolder, "pig/sfAndTotalCounts"),
        wikiClosure=wikipediaToDBpediaClosure,
        resStore
      )
    )

    val sfStore  = MemoryStore.loadSurfaceFormStore(new FileInputStream(new File(modelDataFolder, "sf.mem")))

    memoryIndexer.addCandidatesByID(
      CandidateMapSource.fromPigFiles(
        new File(rawDataFolder, "pig/pairCounts"),
        wikipediaToDBpediaClosure,
        resStore,
        sfStore
      ),
      sfStore.size
    )

    memoryIndexer.addTokenTypes(
      TokenSource.fromPigFile(
        new File(rawDataFolder, "pig/token_counts")
      )
    )

    val tokenStore = MemoryStore.loadTokenTypeStore(new FileInputStream(new File(modelDataFolder, "tokens.mem")))

    memoryIndexer.createContextStore(resStore.size)
    memoryIndexer.addTokenOccurrences(
      TokenOccurrenceSource.fromPigFile(
        new File(rawDataFolder, "pig/token_counts"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      )
    )
    memoryIndexer.writeTokenOccurrences()


  }

}
