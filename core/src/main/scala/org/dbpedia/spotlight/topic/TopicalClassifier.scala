package org.dbpedia.spotlight.topic

import org.dbpedia.spotlight.model.{Topic, Text}
import scala.collection.mutable._


/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 5/30/12
 * Time: 2:38 PM
 * To change this template use File | Settings | File Templates.
 */
trait TopicalClassifier {

  def getPredictions(text:Text):Array[(Topic,Double)]
  def getPredictions(ids: Array[Int],values:Array[Double]) :Array[(Topic,Double)]
  def getTopics():List[Topic]
  def update(text:Text, topic: Topic, increaseVocabulary: Int = 0)
  def update(vector:Map[Int,Double], topic: Topic)
  def persist
}
