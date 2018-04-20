package org.dbpedia.spotlight.relevance

import org.dbpedia.spotlight.model._
import scala.Predef._
import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

/**
 * A class for determining the relevance of the spotted entity in the text.
 * Scores are usually determined by using vectors (words => counts) of:
 * (i) the context of an entity
 * (i) the text
 * @author David Przybilla (david.przybilla@idioplatform.com)
 * @author Thiago Galery (thiago.galery@idioplatform.com)
 */
class ContextRelevanceScore extends RelevanceScore  {

  /*
  * returns a normalized version of vector
  * */
  def normalizeVector(vector:Map[TokenType, Int]):Map[TokenType, Double]={
    val totalSumOfTokens = vector.values.sum
    var normalizedVector = mutable.Map[TokenType, Double]()
    for( (token, counts) <- vector){
      val normalizedCount = counts / totalSumOfTokens.toDouble
      normalizedVector(token) = normalizedCount
    }

    return normalizedVector.toMap
  }

  /*
  * Pre-processes a vector so that:
  * - vector contains maxNumberOfDimensions whose frequencies are the highest.
  * - vector is normalized
  * 
  * */
  def processTextVector(vector:java.util.Map[TokenType, Int],
                       maxNumberOfDimensions:Int
                       ):Map[TokenType, Double]={
    //Prune Dimensions
    val prunedVector = vector.asScala.toSeq.sortBy(_._2).reverse.slice(0, maxNumberOfDimensions).toMap
    //Normalize
    val normalizedVector:Map[TokenType, Double] = normalizeVector(prunedVector)
    return normalizedVector
  }


  /*
  * A method for determining the overlap between entity context and the text, by:
  * - casting context vectors from Java types to Scala types
  * - pruning context vectors based on the tokens found in the text
  *
  * @param textVector frequency of stemmed tokens in text
  * @param contextVector context vector of a topic
  * @param maxDimensions max number of dimensions of the output vector
  * */
  def getTextContextOverlap(textVector:Map[TokenType, Double],
                            contextVector:java.util.Map[TokenType, Int],
                            maxDimensions:Int
                            ):Map[TokenType, Double]={
    var contextVectorScala = contextVector.asScala
    val topContext = processTextVector(contextVector, maxDimensions)
    val tokensIntersection = textVector.keySet.intersect(contextVectorScala.keySet).union(topContext.keySet)
    contextVectorScala = contextVectorScala.filter(tokcntPair => tokensIntersection.contains(tokcntPair._1))
    val processedContextVector = processTextVector(contextVectorScala.asJava.asInstanceOf[java.util.Map[TokenType, Int]],
                                                  tokensIntersection.size)
    return processedContextVector
  }

  /*
  * Returns a map containing #TopicsContainingContextWord / #TotalNumberOfTopics
  * @param textVector  frequency of stemmed tokens in text
  * @param topicContextVectors context vectors of topics
  * */
  def topicFrequency(textVector:Map[TokenType, Double],
                     topicContextVectors:Map[DBpediaResource,
                     Map[TokenType, Double]]
                     ):Map[TokenType, Double]={

    var tfMap = mutable.Map[TokenType, Double]()
    val totalDocs = topicContextVectors.size.toDouble

    for((tokenType:TokenType, textCounts:Double) <- textVector){
      var counts = 0
      for ((dbpediaResource, contextVector)<-topicContextVectors){
        if (contextVector.contains(tokenType))
          counts = counts + 1
      }
      tfMap(tokenType) = counts/totalDocs.toDouble
    }
    return tfMap.toMap
  }

  /*
  * Transforms currentValue to a new value given the parameters for a max-min normalization
  * @param currentValue value to transform
  * @param minValue min value of the sequences of numbers being transformed
  * @param maxValue max value  of the sequences of numbers being transformed
  * @param newMinValue the new min value after normalization
  * @param newMaxValue the new max value after normalization
  * */
  def getMinMaxNormalizationValue(currentValue:Double,
                                  minValue:Double,
                                  maxValue:Double,
                                  newMinValue:Double,
                                  newMaxValue:Double
                                  ):Double ={
    if (minValue != maxValue)
      return ((currentValue - minValue) / (maxValue-minValue)) * (newMaxValue-newMinValue) + newMinValue
    else
      return newMaxValue
  }

  /*
  * Calculates the relevance for a topic given its contextVector and the textVector.
  * @param tokenOverlap the tokens overlapping between text vector and context vector
  * @param contextVector the context vector of the current topic
  * @param tfMap the frequency of tokens among  the context vectors of spotted topics
  * @param frequencyOfTopicInText number of times the topic was spotted in the text
  * */
  def calculateRelevance(tokenOverlap:Set[TokenType],
                         contextVector:Map[TokenType, Double],
                         textVector:Map[TokenType, Double],
                         tfMap:Map[TokenType, Double],
                         frequencyOfTopicInText:Double
                         ):Double = {
      var score = 0.0

      // adding score for common tokens in context
      for(tokenType<-tokenOverlap){
        // Strength of word for the current topic
        val topicScore = contextVector.getOrElse(tokenType, 0.0)

        // Strength of word in contextVector and in the actual text
        val boostScoreContext = topicScore * textVector.getOrElse(tokenType,0.0)

        // An extra boost for context words which are shared among other topics spotted
        // Basically this work on the assumption that relevant topics in an article are within domain;
        // so those topics should share some of their context words.
        val boostCommonTokenAmongTopics = topicScore  *  tfMap.getOrElse(tokenType,0.0) * 0.4
        score = score + (topicScore + boostScoreContext + boostCommonTokenAmongTopics)
      }

     // adding boost based on # of times topic is in text
     score = score + ( (1.0 - score)*(frequencyOfTopicInText) )

    return score

  }

  /*
  * @param contextVectors context vectors of spotted topics
  * @param textVector frequency of stemmed tokens in text
  * @param tfMap the frequency of tokens among  the context vectors of spotted topics
  * @param frequencyOfTopicsInText frequency table telling how many times a topic is spotted in the text
  *
  * Returns a hash table matching topics to relevance scores
  * */
  def getRelevances(contextVectors:Map[DBpediaResource,Map[TokenType, Double]],
                    textVector:Map[TokenType, Double],
                    tfMap:Map[TokenType, Double],
                    frequencyOfTopicsInText:Map[DBpediaResource, Int]
                    ):mutable.Map[DBpediaResource, Double]={

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
      scores(dbpediaTopic) = getMinMaxNormalizationValue(scores(dbpediaTopic), minValue, maxValue, 0.1, newMaxValue)
    }

    return scores

  }

  /*
  *  Calculates the relevance score
  *
  *  @param textVector frequency of stemmed tokens in text
  *  @param contextTopicVectors map of topic to contextVector
  *  @param frequencyOfTopicsInText frequency table telling how many times a topic is spotted in the text
  * */
  def getRelevanceScores(textVector: Map[TokenType, Int],
                         contextTopicVectors: Map[DBpediaResource, java.util.Map[TokenType, Int]],
                         frequencyOfTopicsInText: Map[DBpediaResource, Int]
                         ): mutable.Map[DBpediaResource, Double]={
    // pre-process Text Vector
    val cleanedTextVector = processTextVector(textVector, 100)

    // pre-process Context Vectors
    val cleanedContextVectors  = mutable.Map[DBpediaResource,Map[TokenType, Double]]()

    for( (dbpediaResource, contextVector) <- contextTopicVectors){
        cleanedContextVectors(dbpediaResource) = getTextContextOverlap(cleanedTextVector, contextVector, 100)
    }

    val tfMap = topicFrequency(cleanedTextVector, cleanedContextVectors.toMap)

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
