package org.dbpedia.spotlight.relevance

import org.dbpedia.spotlight.model._
import scala.collection.mutable.ListBuffer
import scala.Predef._
import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.db.model.ContextStore
import org.dbpedia.spotlight.relevance.Relevance.RelevanceScore

/**
 * Created by dav009 on 03/02/2014.
 */
class BaseRelevanceScore extends RelevanceScore  {

  /*
  * returns a normalized version of vector
  * */
  def normalizeVector(vector:Map[TokenType,Int]):Map[TokenType,Double]={
    val totalSumOfTokens = vector.values.sum
    var normalizedVector = mutable.Map[TokenType,Double]()
    for( (token, counts) <- vector){
      val normalizedCount = counts / totalSumOfTokens.toDouble
      normalizedVector(token) = normalizedCount
    }

    return normalizedVector.toMap
  }

  /*
  * Preprocess a vector so that:
  * - vector contains maxNumberOfDimensions whose frequencies are the highest.
  * - vector is normalized
  * */
  def preprocessVector(vector:java.util.Map[TokenType,Int], maxNumberOfDimensions:Int):Map[TokenType, Double]={
    //Prune Dimensions
    val prunedVector = vector.asScala.toSeq.sortBy(_._2).reverse.slice(0, maxNumberOfDimensions).toMap
    //Normalize
    val normalizedVector:Map[TokenType, Double] = normalizeVector(prunedVector)
    return normalizedVector
  }

  def processContextVectors(textVector:Map[TokenType, Double], contextVector:java.util.Map[TokenType, Int], maxDimensions:Int):Map[TokenType, Double]={
    var contextVectorScala = contextVector.asScala
    val tokensInteresction = textVector.keySet.intersect(contextVectorScala.keySet)
    contextVectorScala = contextVectorScala.filter(tokcntPair => tokensInteresction.contains(tokcntPair._1))
    val processedContextVector = preprocessVector(contextVectorScala.asJava.asInstanceOf[java.util.Map[TokenType, Int]], maxDimensions)
    return processedContextVector
  }

  /*
  * returns a map containing NumberOfTopicsContainingContextWordi / #TotalNumberOfTopics
  * */
  def topicFrequency(textVector:Map[TokenType,Double],topicContextVectors:Map[DBpediaResource,Map[TokenType,Double]]):Map[TokenType, Double]={
    var tfMap = mutable.Map[TokenType, Double]()
    val totalDocs = topicContextVectors.size.toDouble

    for((tokenType:TokenType, textCounts:Double) <- textVector){
      var counts = 0
      for ((dbpediaResource, contextVector)<-topicContextVectors){
        if (contextVector.contains(tokenType))
          counts = counts + 1
      }
      if (counts > 0)
        tfMap(tokenType) = counts/totalDocs.toDouble
      else
        tfMap(tokenType) = 0.0
    }
    return tfMap.toMap
  }

  def getMinMaxNormalizationValue(currentValue:Double, minValue:Double, maxValue:Double, newMinValue:Double, newMaxValue:Double):Double ={
    if (minValue!=maxValue)
      return ((currentValue - minValue) / (maxValue-minValue)) * (newMaxValue-newMinValue) + newMinValue
    else
      return newMaxValue
  }

  /*
  * Calculates the relevance for a topic given its contextVector and the textVector
  * */
  def calculateRelevance(tokenOverlap:Set[TokenType],contextVector:Map[TokenType,Double], textVector:Map[TokenType,Double], tfMap:Map[TokenType, Double], frequencyOfTopicInText:Double):Double = {
      var score = 0.0

      // adding score for common tokens in context
      for(tokenType<-tokenOverlap){
        /*
        * Strength of word for the current topic
        * */
        val topicScore =  contextVector.getOrElse(tokenType,0.0)
        /*
        * Strength of word in contextVector and in the actual text
        * */
        val boostScoreContext =  topicScore * textVector.getOrElse(tokenType,0.0)
        /*
        An extra boost for context words which are shared among other topics spotted. ]
        Basically this work on the assumption that the relevant topics in an article are usually around one domain
        so those topics should share some of their context words.
        */
        val boostCommonTokenAmongTopics = topicScore  *  tfMap.getOrElse(tokenType,0.0) * 0.4
        score = score + (topicScore + boostScoreContext + boostCommonTokenAmongTopics)
      }

     // adding boost based on # of times topic is in text
     score = score + ( (1 -score)*(frequencyOfTopicInText) )

    return score

  }

  def getRelevances(contextVectors:Map[DBpediaResource,Map[TokenType,Double]], textVector:Map[TokenType,Double], tfMap:Map[TokenType, Double], frequencyOfTopicsInText:Map[DBpediaResource, Int]):mutable.Map[DBpediaResource, Double]={
    val scores = mutable.HashMap[DBpediaResource, Double]()
    val sumOfTopicFrequencys:Int= frequencyOfTopicsInText.values.map(_.toInt).sum

    // Calculating the score for each topic
    for( (dbpediaResource, contextVector)<-contextVectors ){
      val tokenOverlap = textVector.keySet.intersect(contextVector.keySet)
      val probabilityOfTopicInText =  frequencyOfTopicsInText(dbpediaResource) / sumOfTopicFrequencys.toDouble
      scores(dbpediaResource) = calculateRelevance(tokenOverlap, contextVector, textVector, tfMap, probabilityOfTopicInText)
    }

    //force normalization
    val minValue = scores.values.min
    val maxValue = scores.values.max

    // trick or treat!
    val newMaxValue = (maxValue + 2.0)/3.0

    scores.keys foreach{ dbpediaTopic: DBpediaResource =>
      //new min value score is 0.1
      //new max value is newMaxValue
      scores(dbpediaTopic) = getMinMaxNormalizationValue(scores(dbpediaTopic), minValue, maxValue,0.1, newMaxValue)
    }

    println("minValue:"+minValue)
    println("maxValue"+maxValue)
    println("FINAL SCORES::::::::")
    val orderedScores = scores.toSeq.sortBy(_._2)
    for( (dbpediaTopic, score)<-orderedScores ){
      println(dbpediaTopic.uri+" -- "+score)
    }

    return scores

  }

  /*
  * Calcualtes the relevance Score(Interface)
  * */
  def score(textVector: java.util.Map[TokenType, Int], contextTopicVectors: Map[DBpediaResource, java.util.Map[TokenType, Int]], frequencyOfTopicsInText: Map[DBpediaResource, Int]): mutable.Map[DBpediaResource, Double]={
    // preprocess Text Vector
    val cleanedTextVector = preprocessVector(textVector, 100)

    // pre-process Context Vectors
    val cleanedContextVectors  = mutable.Map[DBpediaResource,Map[TokenType, Double]]()

    for( (dbpediaResource, contextVector) <- contextTopicVectors){
        cleanedContextVectors(dbpediaResource) = processContextVectors(cleanedTextVector, contextVector, 100)
    }

    val tfMap =  topicFrequency(cleanedTextVector, cleanedContextVectors.toMap)

    // Calculate Scores
    return getRelevances(cleanedContextVectors.toMap, cleanedTextVector, tfMap, frequencyOfTopicsInText)
  }

  /*
  * Not used
  * */
  def nilScore(query: java.util.Map[TokenType, Int]): Double={
    return 0.0
  }


}
