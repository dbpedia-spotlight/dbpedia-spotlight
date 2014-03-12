package org.dbpedia.spotlight.db.similarity

import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}
import collection.mutable
import org.dbpedia.spotlight.db.model.{ContextStore, TokenTypeStore}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.util.MathUtil
import org.apache.commons.logging.LogFactory

/**
 * Generative context similarity based on Han et. al
 *
 * X. Han and L. Sun. A generative entity-mention model for linking entities with knowledge base.
 * In Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human
 * Language Technologies-Volume 1, pages 945–954. Association for Computational Linguistics, 2011.
 *
 * @author Joachim Daiber
 */

class GenerativeContextSimilarity(tokenTypeStore: TokenTypeStore, contextStore: ContextStore) extends ContextSimilarity {

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

    MathUtil.ln(token.count + 1.0) - MathUtil.ln(tokenTypeStore.getTotalTokenCount + tokenTypeStore.getVocabularySize)
  }


  /**
   * Calculate the probability of a token in the context of the DBpedia resource.
   *
   * @param token the token
   * @param res the DBpedia resource
   * @return
   */
  def p(token: TokenType, res: DBpediaResource, cResAndToken: Int): Double = {

    val pML = if (cResAndToken == 0 || contextStore.getTotalTokenCount(res) == 0 )
      0.0
    else
      cResAndToken.toDouble / contextStore.getTotalTokenCount(res)

    val ml = MathUtil.lnproduct(MathUtil.ln(lambda), MathUtil.ln(pML))
    val lm = MathUtil.lnproduct(MathUtil.ln(1-lambda), pLM(token))

    MathUtil.lnsum( lm, if(ml.isNaN) MathUtil.LOGZERO else ml )
  }

  def intersect(query: Seq[TokenType], res: DBpediaResource): Seq[(TokenType, Int)] = {
    val (tokens, counts) = contextStore.getRawContextCounts(res)
    if (tokens.length == 0) {
      query.map( t => (t, 0))
    } else {
      var j = 0
      query.map { t: TokenType =>
        while(j < tokens.length-1 && tokens(j) < t.id) {
          j += 1
        }

        if(tokens(j) == t.id) (t, counts(j))
        else (t, 0)
      }
    }

  }


  def score(query: Seq[TokenType], candidates: Set[DBpediaResource]): mutable.Map[DBpediaResource, Double] = {
    val contextScores = mutable.HashMap[DBpediaResource, Double]()

    candidates.map( res => {
      contextScores.put(
        res,
        MathUtil.lnproduct(
          intersect(query, res).map({ case(token: TokenType, cResAndToken: Int) =>p(token, res, cResAndToken) })
            .filter(s => !MathUtil.isLogZero(s))
        )
      )
    })
    contextScores
  }


  def nilScore(query: Seq[TokenType]): Double = {
    MathUtil.lnproduct(
      query.map{ t: TokenType =>
        MathUtil.lnproduct(MathUtil.ln(1-lambda), pLM(t))
      }
    )
  }

}