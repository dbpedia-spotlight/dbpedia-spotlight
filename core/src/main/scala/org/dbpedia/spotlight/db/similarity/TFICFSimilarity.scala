package org.dbpedia.spotlight.db.similarity

import scala.collection.JavaConversions._
import collection.mutable.HashMap
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{DBpediaResource, Candidate, Token}
import scala.Int

/**
 * @author Joachim Daiber
 *
 */

class TFICFSimilarity extends ContextSimilarity {

  private val LOG = LogFactory.getLog(this.getClass)

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
      math.log(nCand / nCandWithToken.toDouble) + 1.0
  }

  def tficf(token: Token, document: java.util.Map[Token, Int], allDocuments: Iterable[java.util.Map[Token, Int]]): Double = {
    tf(token, document).toDouble * icf(token, document, allDocuments)
  }


  def score(query: java.util.Map[Token, Int], candidateContexts: Map[DBpediaResource, java.util.Map[Token, Int]]): Map[DBpediaResource, Double] = {

    val allDocs = candidateContexts.values
    val scores = HashMap[DBpediaResource, Double]()
    query.keys.foreach{
      token => {
        candidateContexts.keys foreach { candRes: DBpediaResource =>
          scores.put(candRes, scores.getOrElse(candRes, 0.0) + tficf(token, candidateContexts(candRes), allDocs))
        }
      }
    }
    candidateContexts.keys foreach { candRes: DBpediaResource =>
      scores.put(candRes, scores(candRes) / candidateContexts(candRes).size().toDouble)
    }

    scores.toMap
  }


}
