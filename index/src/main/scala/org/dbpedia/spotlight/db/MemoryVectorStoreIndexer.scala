package org.dbpedia.spotlight.db

import java.io.File
import breeze.linalg.DenseMatrix
import org.dbpedia.spotlight.db.model.{ResourceStore, TokenTypeStore}

import scala.collection.immutable.Iterable
import scala.io.Source
import java.util

import org.dbpedia.spotlight.db.memory.{MemoryStore, MemoryVectorStore}
import org.dbpedia.spotlight.model.{TokenType, DBpediaResource, TokenOccurrenceIndexer}

/**
 * Created by dowling on 09/07/15.
 */
class MemoryVectorStoreIndexer(val baseDir: File, modelPath: String, dictPath: String){
  lazy val contextStore = new MemoryVectorStore()

  var dict: Map[String, Int] = null

  def loadVectorDict(tokenTypeStore: TokenTypeStore, resourceStore: ResourceStore) = {
    dict = Source.fromFile(dictPath, "UTF-8").getLines().map { line =>
      val contents = line.split("\t")
      (contents(0), contents(1).toInt)
    }.toMap
    var resources: collection.mutable.Map[Int, Int] = collection.mutable.HashMap[Int,Int]()
    var tokens: collection.mutable.Map[Int, Int] = collection.mutable.HashMap[Int,Int]()
    // TODO: error handling if we can't find the token or resource
    dict.foreach { case(key, value) =>
      if(key.startsWith("DBPEDIA_ID/")){
        val resource = resourceStore.getResourceByName(key.replace("DBPEDIA_ID/", ""))
        resources += (resource.id -> value)
      }else{
        val token = tokenTypeStore.getTokenType(key)
        tokens += (token.id -> value)
      }
    }
    contextStore.resourceIdToVectorIndex = resources.toMap
    contextStore.tokenTypeIdToVectorIndex = tokens.toMap
  }

  def loadVectorsAndWriteToStore() = {
    val matrixSource = Source.fromFile(modelPath)
    val lines = matrixSource.getLines()
    val rows = lines.next().substring(2).toInt
    val cols = lines.next().substring(2).toInt
    contextStore.vectors = new DenseMatrix[Float](rows, cols)
    println("Reading CSV and writing to store...")
    lines.zipWithIndex.foreach { case (row_str, row_idx) =>
      if (row_idx % 10000 == 0)
        println("At row " + row_idx)
      val values = row_str.split(",").map(_.trim).map(_.toDouble)
      values.zipWithIndex.foreach { case (value, col_idx) =>
        contextStore.vectors(row_idx, col_idx) = value.toFloat
      }
    }
    matrixSource.close()
    println("Done, dumping..")
    MemoryStore.dump(contextStore, new File(baseDir, "vectors.mem"))
  }

}
