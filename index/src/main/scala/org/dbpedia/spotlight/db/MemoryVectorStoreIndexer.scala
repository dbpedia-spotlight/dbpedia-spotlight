package org.dbpedia.spotlight.db

import java.io.File
import breeze.linalg.DenseMatrix
import org.dbpedia.spotlight.db.model.{ResourceStore, TokenTypeStore}
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException
import org.dbpedia.spotlight.log.SpotlightLog

import scala.collection.immutable.Iterable
import scala.io.Source
import java.util

import org.dbpedia.spotlight.db.memory.{MemoryStore, MemoryVectorStore}
import org.dbpedia.spotlight.model.{TokenType, DBpediaResource, TokenOccurrenceIndexer}

/**
 * Created by dowling on 09/07/15.
 */
class MemoryVectorStoreIndexer(modelPath: File, dictPath: File){
  lazy val contextStore = new MemoryVectorStore()

  var dict: Map[String, Int] = null

  def loadVectorDict(tokenTypeStore: TokenTypeStore, resourceStore: ResourceStore) = {
    SpotlightLog.info(this.getClass,"Loading vector dictionary!")
    dict = Source.fromFile(dictPath, "UTF-8").getLines().map { line =>
      val contents = line.split("\t")
      (contents(0), contents(1).toInt)
    }.toMap
    var resources: collection.mutable.Map[Int, Int] = collection.mutable.HashMap[Int,Int]()
    var tokens: collection.mutable.Map[Int, Int] = collection.mutable.HashMap[Int,Int]()
    // TODO: error handling if we can't find the token or resource
    var failedResources = 0
    var succeededResources = 0
    var failedTokens = 0
    var succeededTokens = 0
    dict.foreach { case(key, value) =>
      if(key.startsWith("DBPEDIA_ID/")){
        try {
          val resource = resourceStore.getResourceByName(key.replace("DBPEDIA_ID/", ""))
          resources += (resource.id -> value)
          succeededResources += 1
        } catch {
          case e: DBpediaResourceNotFoundException=> {
            failedResources += 1
            if (failedResources % 1000 == 0){
              SpotlightLog.debug(this.getClass,"Can't find resource: " + key)
            }
          }
        }
      }else{
        val token = tokenTypeStore.getTokenType(key)
        if (token == TokenType.UNKNOWN){
          failedTokens += 1
          if (failedTokens % 1000 == 0){
            SpotlightLog.debug(this.getClass,"Can't find token: " + key)
          }
        } else {
          tokens += (token.id -> value)
          succeededTokens += 1
        }
      }
    }
    SpotlightLog.info(this.getClass,"Failed on " + failedResources + " entities, succeeded on " + succeededResources)
    SpotlightLog.info(this.getClass,"Failed on " + failedTokens + " tokens, succeeded on " + succeededTokens)
    contextStore.resourceIdToVectorIndex = resources.toMap
    contextStore.tokenTypeIdToVectorIndex = tokens.toMap
    SpotlightLog.info(this.getClass,"Done loading dict.")
  }

  def loadVectorsAndWriteToStore(outputFile:File) = {
    SpotlightLog.info(this.getClass,"Loading vectors..")
    val matrixSource = Source.fromFile(modelPath)
    val lines = matrixSource.getLines()
    val rows = lines.next().substring(2).toInt
    val cols = lines.next().substring(2).toInt
    contextStore.vectors = new DenseMatrix[Float](rows, cols)
    SpotlightLog.info(this.getClass,"Reading CSV and writing to store...")
    lines.zipWithIndex.foreach { case (row_str, row_idx) =>
      if (row_idx % 10000 == 0)
        SpotlightLog.debug(this.getClass,"At row " + row_idx)
      val values = row_str.split(",").map(_.trim).map(_.toDouble)
      values.zipWithIndex.foreach { case (value, col_idx) =>
        contextStore.vectors(row_idx, col_idx) = value.toFloat
      }
    }
    matrixSource.close()
    SpotlightLog.info(this.getClass,"Done, dumping..")
    MemoryStore.dump(contextStore, outputFile)
  }

}
