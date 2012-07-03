package org.dbpedia.spotlight.db.similarity

import org.dbpedia.spotlight.model.{Candidate, Token}
import scala.collection.JavaConversions._

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class TFICFSimilarity extends ContextSimilarity {

  def tf(token: Token, document: java.util.Map[Token, Int]) = {
    document.get(token) match {
      case c: Int => c
      case _ => 0
    }
  }

  def icf(token: Token, document: java.util.Map[Token, Int], allDocuments: Set[java.util.Map[Token, Int]]): Double = {

    val nCandWithToken = allDocuments.map{ doc =>
      doc.get(token) match {
        case c: Int => c
        case _ => 0
      }
    }.filter( count => count > 0 ).size - 1 //also contains the query

    val nCand = allDocuments.size - 1 //also contains the query

    if (nCandWithToken == 0)
      0.0
    else
      math.log(nCand / (nCandWithToken)) + 1.0
  }

  def tficf(token: Token, document: java.util.Map[Token, Int], allDocuments: Set[java.util.Map[Token, Int]]): Double = {
    tf(token, document).toDouble * icf(token, document, allDocuments)
  }


  def score(query: java.util.Map[Token, Int], candidate: Candidate, contextCounts: Map[Candidate, java.util.Map[Token, Int]]): Double = {

    val allDocs = Set(query) ++ contextCounts.values
    val doc = contextCounts(candidate)
    val mergedTokens = query.keySet().intersect(doc.keySet())

    val a = mergedTokens.map{ t: Token => tficf(t, query, allDocs) * tficf(t, doc, allDocs) }.sum
    val b = math.sqrt( query.keySet.map{ t: Token => math.pow(tficf(t, query, allDocs), 2) }.sum )
    val c = math.sqrt( doc.keySet.map{   t: Token => math.pow(tficf(t, doc, allDocs),   2) }.sum )

    a / (b * c).toDouble
  }


}
