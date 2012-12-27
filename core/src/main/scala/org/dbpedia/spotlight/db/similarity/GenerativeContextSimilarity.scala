package org.dbpedia.spotlight.db.similarity

import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}
import collection.mutable
import org.dbpedia.spotlight.db.model.TokenTypeStore
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.util.MathUtil
import org.apache.commons.logging.LogFactory

/**
 * Generative context similarity based on Han et. al
 *
 * X. Han and L. Sun. A generative entity-mention model for linking entities with knowledge base.
 * In Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human
 * Language Technologies-Volume 1, pages 945–954. Association for Computational Linguistics, 2011.
 *
 * @author Joachim Daiber
 */

class GenerativeContextSimilarity(tokenTypeStore: TokenTypeStore) extends ContextSimilarity {

  /**
   * Weight for smoothing the token probability with the general language model probability.
   *
   * TODO: may need to be re-estimated for new languages
   */
  val lambda = 0.2

  /**
   * Calculate a smoothed LM probability for a single token.
   *
   * @param token the token
   * @return
   */
  def pLM(token: TokenType): Double = {
    /* TODO: We use simple Laplace smoothing here because it does not require heldout estimation,
     but a more advanced smoothing method may be used here. */

    //(token.count + 1.0) / (tokenTypeStore.getTotalTokenCount + tokenTypeStore.getVocabularySize)
    MathUtil.ln(token.count + 1.0) - MathUtil.ln(tokenTypeStore.getTotalTokenCount + tokenTypeStore.getVocabularySize)
  }

  /**
   * Calculate the probability of a token in the context of the DBpedia resource.
   *
   * @param token the token
   * @param res the DBpedia resource
   * @param contextCounts Map of token counts for the DBpedia resource
   * @param totalContextCount total count of tokens for the DBpedia resource
   * @return
   */
  def p(token: TokenType, res: DBpediaResource, contextCounts: java.util.Map[TokenType, Int], totalContextCount: Int): Double = {

    val pML = try {
      contextCounts.get(token).toDouble / totalContextCount
    } catch {
      case e: ArithmeticException => 0.0
    }

    MathUtil.lnsum(MathUtil.lnproduct(MathUtil.ln(lambda), MathUtil.ln(pML)), MathUtil.lnproduct(MathUtil.ln(1-lambda), pLM(token)))
  }

  def score(query: java.util.Map[TokenType, Int], contextCounts: Map[DBpediaResource, java.util.Map[TokenType, Int]], totalContextCounts: Map[DBpediaResource, Int]): mutable.Map[DBpediaResource, Double] = {
    val contextScores = mutable.HashMap[DBpediaResource, Double]()
    contextCounts.keys.map( res => {
      contextScores.put(
        res,
        MathUtil.lnproduct(
          query.map({ case(t: TokenType, c: Int) => MathUtil.lnproduct(MathUtil.ln(c.toDouble), p(t, res, contextCounts.get(res).get, totalContextCounts.get(res).get)) })
            .filter({ s: Double => !MathUtil.isLogZero(s)})
        )
      )
    })
    contextScores
  }

}