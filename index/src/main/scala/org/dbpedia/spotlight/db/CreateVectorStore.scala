package org.dbpedia.spotlight.db

import java.io.{FileInputStream, File}

import org.dbpedia.spotlight.db.memory.{MemoryQuantizedCountStore, MemoryStore}

/**
 * Created by dowling on 14/08/15.
 */
object CreateVectorStore {
  def main (args: Array[String]) {
    val (modelPathAndPrefix: File, outputFolder: File) = try {
      (
        new File(args(0)),
        new File(args(1))
        )
    } catch {
      case e: Exception => {
        e.printStackTrace()
        System.err.println("Usage:")
        System.err.println("    mvn scala:run -DmainClass=org.dbpedia.spotlight.db.CreateVectorStore -Dexec.args=\"/data/input/word2vecmodel /data/output\"")
        System.exit(1)
      }
    }
    val modelDataFolder = new File(outputFolder, "model")
    val quantizedCountStore = new MemoryQuantizedCountStore()
    val resStore = MemoryStore.loadResourceStore(new FileInputStream(new File(modelDataFolder, "res.mem")), quantizedCountStore)
    val tokenStore = MemoryStore.loadTokenTypeStore(new FileInputStream(new File(modelDataFolder, "tokens.mem")))
    val memoryVectorStoreIndexer = new MemoryVectorStoreIndexer(
      new File(modelPathAndPrefix + ".syn0.csv"),
      new File(modelPathAndPrefix + ".wordids.txt")
    )
    memoryVectorStoreIndexer.loadVectorDict(tokenStore, resStore)
    memoryVectorStoreIndexer.loadVectorsAndWriteToStore(new File(modelDataFolder, "vectors.mem"))

  }


}
