package org.dbpedia.spotlight.model

import collection.mutable.HashMap

/**
 * @author Joachim Daiber
 */

trait HasFeatures {

  val features = HashMap[String, Feature]()

  def feature(featureName: String) : Option[Feature] = {
    features.get(featureName)
  }

  def featureValue[T](featureName: String): Option[T] = {
    features.get(featureName) match {
      case Some(f) => Option(f.value.asInstanceOf[T])
      case None => None
    }
  }

  def setFeature(feature: Feature) {
    features.put(feature.featureName, feature)
  }

}
