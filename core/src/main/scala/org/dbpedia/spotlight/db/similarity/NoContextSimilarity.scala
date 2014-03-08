package org.dbpedia.spotlight.db.similarity

import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}
import scala.collection.mutable
import org.dbpedia.spotlight.util.MathUtil

/**
 * Created by dav009 on 06/03/2014.
 */
class NoContextSimilarity(val defaultScoreValue:Double) extends ContextSimilarity{

  def score(query: Seq[TokenType], candidates: Set[DBpediaResource]): mutable.Map[DBpediaResource, Double] ={

    val contextScores = mutable.Map[DBpediaResource, Double]().withDefaultValue(defaultScoreValue)

    contextScores
  }

  def nilScore(query: Seq[TokenType]): Double ={
    MathUtil.LOGZERO
  }
}
