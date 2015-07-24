package org.dbpedia.spotlight.db.similarity

import org.dbpedia.spotlight.db.memory.MemoryVectorStore
import org.dbpedia.spotlight.db.model.{TokenTypeStore, ContextStore}

import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}
import org.dbpedia.spotlight.util.MathUtil
import org.dbpedia.spotlight.util.MathUtil.{cosineSimilarity, LOGZERO}

import scala.collection.mutable


/**
 * Context similarity based on dense, continuous space vector models.
 * @author Philipp Dowling
 *
 * created on 12/06/15.
 */
case class VectorContextSimilarity(tokenTypeStore: TokenTypeStore, memoryVectorStore: MemoryVectorStore) extends ContextSimilarity{

  def vectorSimilarities(query: Seq[TokenType], candidates: Set[DBpediaResource]): mutable.Map[DBpediaResource, Double] = {
    val contextScores = mutable.HashMap[DBpediaResource, Double]()

    candidates.map( resource => {
      contextScores.put(
        resource,
        // similarity of context and current resource
        // TODO: use the whole context, or only surrounding n words?
        MathUtil.ln(
          cosineSimilarity(
            query.map(memoryVectorStore.lookup).reduceLeft(_ + _),
            memoryVectorStore.lookup(resource)
          )
        )
      )
    }
    )
    contextScores
  }

  val lambda = 0.2

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
   * Calculate a smoothed LM probability for a single token.
   *
   * @param token the token
   * @return
   */
  def pLM(token: TokenType): Double = {
    /* TODO: We use simple Laplace smoothing here because it does not require heldout estimation,
     but a more advanced smoothing method may be used here. */

    MathUtil.ln(token.count + 1.0) - MathUtil.ln(tokenTypeStore.getTotalTokenCount + tokenTypeStore.getVocabularySize)
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
  def nilScore(query: Seq[TokenType]): Double = {
    MathUtil.lnproduct(
      query.map{ t: TokenType =>
        MathUtil.lnproduct(MathUtil.ln(1-lambda), pLM(t))
      }
    )
    LOGZERO
  }
}
