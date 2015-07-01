package org.dbpedia.spotlight.db.similarity

import java.io.File

import breeze.linalg._
import breeze.numerics._

import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}
import org.dbpedia.spotlight.util.MathUtil.{cosineSimilarity, LOGZERO}

import scala.collection.mutable
import scala.io.Source



/**
 * Context similarity based on dense, continuous space vector models.
 * @author Philipp Dowling
 *
 * created on 12/06/15.
 */
case class VectorContextSimilarity(modelPath: String, dictPath: String) extends ContextSimilarity{
  var vectors = readWeightsCsv

  println("Read dict..")
  var dict: Map[String, Int] = Source.fromFile(dictPath, "UTF-8").getLines().map { line =>
    val contents = line.split("\t")
    (contents(0), contents(1).toInt)
  }.toMap

  def readWeightsCsv: DenseMatrix[Double] = {
    val matrixSource = io.Source.fromFile(modelPath)
    val lines = matrixSource.getLines()
    val rows = lines.next().substring(2).toInt
    val cols = lines.next().substring(2).toInt
    println("Allocating " + rows + "x" + cols + " matrix..")
    val vectors: DenseMatrix[Double] = DenseMatrix.zeros[Double](rows, cols) //DenseMatrix.rand[Double](rows, cols)
    println("Reading CSV and writing to matrix...")
    lines.zipWithIndex.foreach { case (row_str, row_idx) =>
      if (row_idx % 10000 == 0)
        println("At row " + row_idx)
      val values = row_str.split(",").map(_.trim).map(_.toDouble)
      values.zipWithIndex.foreach { case (value, col_idx) => vectors(row_idx, col_idx) = value }
    }
    matrixSource.close()
    println("Done.")
    vectors
  }

  def lookup(token: String): Transpose[DenseVector[Double]]={
    // look up vector, if it isn't there, simply ignore the word
    println("Looking up " + token + "..")
    if(dict.contains(token)){
      vectors(dict(token), ::)
    }else{
      // TODO: is this good standard behaviour?
      println("Warning: token " + token + " not in dictionary! Lookup returning null vector.")
      DenseVector.zeros[Double](vectors.cols).t
    }
  }

  def getSimilarity(first: String, second:String): Double = {
    cosineSimilarity(lookup(first), lookup(second))
  }

  def getSimilarity(first: Array[String], second: Array[String]): Double = {
    val f = first.map(lookup).reduceLeft(_ + _)
    val s = second.map(lookup).reduceLeft(_ + _)

    cosineSimilarity(f, s)
  }


  def vectorSimilarities(query: Seq[TokenType], candidates: Set[DBpediaResource]): mutable.Map[DBpediaResource, Double] = {
    val contextScores = mutable.HashMap[DBpediaResource, Double]()

    candidates.map( resource => {
      contextScores.put(
        resource,
        // similarity of context and current resource
        // TODO: use the whole context, or only surrounding n words?
        getSimilarity(Array(resource.getFullUri.replace("http://dbpedia.org/resource/", "DBPEDIA_ID/")), query.map(_.tokenType).toArray)
      )
    }
    )
    contextScores
  }


  /**
   * Calculate the context score for all DBpedia resources in the given text. The text context is specified
   * as q query of tokens and their counts.
   *
   * @param query the text context of the document
   * @param candidates the set of DBpedia resource candidates
   * @return
   */
  override def score(query: Seq[TokenType], candidates: Set[DBpediaResource]): mutable.Map[DBpediaResource, Double] = {
    /**
     * TODO:
     * Currently, this just calculates the dot product of the query vector and the sum of the context vectors.
     * In the future, this should invoke a log-linear model that's been trained to rank candidates based on a number of
     * features, as outlined in the proposal.
     */
    vectorSimilarities(query, candidates)
  }

  /**
   * Calculate the context score for the context alone, not assuming that there is any entity generating it.
   *
   * In the generative model, this is: \product_i P_LM(token_i)
   *
   * TODO: what is this in the log-linear model?
   *
   * @param query the text context of the document
   * @return
   */
  override def nilScore(query: Seq[TokenType]): Double ={
    LOGZERO
  }
}
