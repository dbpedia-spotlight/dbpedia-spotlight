package org.dbpedia.spotlight.db.similarity

import org.dbpedia.spotlight.model.{Candidate, Token}
import scala.collection.JavaConversions._
import collection.mutable.HashMap

/**
 * @author Joachim Daiber
 *
 */

class TFICFSimilarity extends ContextSimilarity {

  def tf(token: Token, document: java.util.Map[Token, Int]) = {
    document.get(token) match {
      case c: Int => c
      case _ => 0
    }
  }

  def icf(token: Token, document: java.util.Map[Token, Int], allDocuments: Iterable[java.util.Map[Token, Int]]): Double = {

    val nCandWithToken = allDocuments.map{ doc =>
      doc.get(token) match {
        case c: Int => c
        case _ => 0
      }
    }.filter( count => count > 0 ).size

    val nCand = allDocuments.size

    if (nCandWithToken == 0)
      0.0
    else
      math.log(nCand / (nCandWithToken)) + 1.0
  }

  def tficf(token: Token, document: java.util.Map[Token, Int], allDocuments: Iterable[java.util.Map[Token, Int]]): Double = {
    tf(token, document).toDouble * icf(token, document, allDocuments)
  }


  def score(query: java.util.Map[Token, Int], candidateContexts: Map[Candidate, java.util.Map[Token, Int]]): HashMap[Candidate, Double] = {

    val allDocs = candidateContexts.values
    val scores = HashMap[Candidate, Double]()
    query.keys.foreach{
      token => {
        candidateContexts.keys foreach { c: Candidate =>
          scores.put(c, scores.getOrElse(c, 0).asInstanceOf[Double] + tficf(token, candidateContexts(c), allDocs))
        }
      }
    }
    candidateContexts.keys foreach { c: Candidate =>
      scores(c) = scores(c) / candidateContexts(c).size()
    }

    scores
  }


}
