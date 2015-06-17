package org.dbpedia.spotlight.db.similarity

import java.io.File

import breeze.linalg._

import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}

import scala.collection.mutable
import scala.io.Source

/**
 * Context similarity based on dense, continuous space vector models.
 * @author Philipp Dowling
 *
 * created on 12/06/15.
 */
class VectorContextSimilarity(modelPath: String, dictPath: String) extends ContextSimilarity{
  var vectors: DenseMatrix[Double] = csvread(new File(modelPath))

  var dict: Map[String, Int] = Source.fromFile(dictPath).getLines().map { line =>
    val contents = line.split("\t")
    (contents(0), contents(1).toInt)
  }.toMap

  def lookup(token: String): Transpose[DenseVector[Double]]={
    // look up vector, if it isn't there, simply ignore the word
    // TODO: is this good standard behaviour?
    if(dict.contains(token)){
      vectors(dict(token), ::)
    }else{
      DenseVector.zeros[Double](vectors.cols).t
    }
  }

  def get_similarity(first: String, second:String): Double = {
    // todo: do we need 1 - (lookup(first) * lookup(second).t) ?

    lookup(first) * lookup(second).t
  }

  def get_similarity(first: Array[String], second: Array[String]): Double = {
    val f = first.map(lookup).reduceLeft(_ + _)
    val s = second.map(lookup).reduceLeft(_ + _)

    f * s.t
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

    val contextScores = mutable.HashMap[DBpediaResource, Double]()

    candidates.map( resource => {
      contextScores.put(
        resource,
        // similarity of context and current resource
        // TODO: use the whole context, or only surrounding n words?
        get_similarity(Array(resource.getFullUri), query.map(_.toString).toArray)
      )
    }
    )
    contextScores
  }

  /**
   * Calculate the context score for the context alone, not assuming that there is any entity generating it.
   *
   * In the generative model, this is: \product_i P_LM(token_i)
   *
   * @param query the text context of the document
   * @return
   */
  override def nilScore(query: Seq[TokenType]): Double = 0.0
}
