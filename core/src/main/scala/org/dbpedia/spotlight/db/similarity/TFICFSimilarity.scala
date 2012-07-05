package org.dbpedia.spotlight.db.similarity

import scala.collection.JavaConversions._

import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{DBpediaResource, Candidate, Token}
import scala.Int
import collection.immutable
import collection.mutable

/**
 * @author Joachim Daiber
 *
 */

class TFICFSimilarity extends ContextSimilarity {

  private val LOG = LogFactory.getLog(this.getClass)

  def tf(token: Token, document: java.util.Map[Token, Int]): Int =
    document.get(token) match {
      case c: Int => c
      case _ => 0
    }

  def icf(token: Token, document: java.util.Map[Token, Int], allDocuments: Iterable[java.util.Map[Token, Int]]): Double = {

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

  def tficf(token: Token, document: java.util.Map[Token, Int], allDocuments: Iterable[java.util.Map[Token, Int]]): Double = {
    tf(token, document).toDouble * icf(token, document, allDocuments)
  }


  def score(query: java.util.Map[Token, Int], candidateContexts: immutable.Map[DBpediaResource, java.util.Map[Token, Int]]): mutable.Map[DBpediaResource, Double] = {

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
      scores(candRes) = scores(candRes) / candidateContexts(candRes).size().toDouble
    }

    scores
  }


}
