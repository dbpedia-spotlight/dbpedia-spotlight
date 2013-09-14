package org.dbpedia.spotlight.db.similarity

import scala.collection.JavaConversions._

import scala.Int
import collection.immutable
import collection.mutable
import org.dbpedia.spotlight.model.{TokenType, DBpediaResource, Candidate, Token}

/**
 * A context similarity model based on TF-ICF (modified TF-IDF).
 *
 * @author Joachim Daiber
 */

class TFICFSimilarity extends ContextSimilarity {

  /**
   * Term frequency of a token in the document.
   *
   * @param token the token
   * @param document the document
   * @return
   */
  def tf(token: TokenType, document: java.util.Map[TokenType, Int]): Int =
    document.get(token) match {
      case c: Int => c
      case _ => 0
    }

  /**
   * Inverse candidate frequency of a token.
   *
   * @param token the token
   * @param document the document (the context of the disambiguated DBpedia resource)
   * @param allDocuments all candidate documents for the query
   * @return
   */
  def icf(token: TokenType, document: java.util.Map[TokenType, Int], allDocuments: Iterable[java.util.Map[TokenType, Int]]): Double = {

    var nCand = 0
    var nCandWithToken = 0
    allDocuments.foreach{ doc =>
      nCand += 1
      doc.get(token) match {
        case c: Int => nCandWithToken += 1
        case _ =>
      }
    }

    if (nCandWithToken == 0)
      0.0
    else
      math.log(nCand / nCandWithToken.toDouble) + 1.0
  }

  /**
   * TF-ICF for the Token.
   *
   * @param token the token
   * @param document the document (the context of the disambiguated DBpedia resource)
   * @param allDocuments all candidate documents for the query
   * @return
   */
  def tficf(token: TokenType, document: java.util.Map[TokenType, Int], allDocuments: Iterable[java.util.Map[TokenType, Int]]): Double = {
    tf(token, document).toDouble * icf(token, document, allDocuments)
  }

  /**
   * Norm value for a document.
   *
   * TODO: Note that this should be the TF-ICF/TF-IDF norm for all documents instead.
   *
   * @param document the document (the context of the disambiguated DBpedia resource)
   * @return
   */
  def norm(document: java.util.Map[TokenType, Int]): Double = {
    document.size().toDouble //math.sqrt( document.keys.map{ v: TokenType => math.pow(v.count, 2) }.sum )
  }

  def score(query: java.util.Map[TokenType, Int], candidateContexts: immutable.Map[DBpediaResource, java.util.Map[TokenType, Int]], totalContextCounts: Map[DBpediaResource, Int]): mutable.Map[DBpediaResource, Double] = {

    val allDocs = candidateContexts.values
    val scores = mutable.HashMap[DBpediaResource, Double]()
    query.foreach{
      case (token, count) => {
        candidateContexts.keys foreach { candRes: DBpediaResource =>
          scores(candRes) = scores.getOrElse(candRes, 0.0) + tficf(token, candidateContexts(candRes), allDocs)
        }
      }
    }
    candidateContexts.keys foreach { candRes: DBpediaResource =>
      scores(candRes) = scores(candRes) / norm(candidateContexts(candRes))
    }

    scores
  }

  def nilScore(query: java.util.Map[TokenType, Int]): Double = {
    0.0
  }

}
