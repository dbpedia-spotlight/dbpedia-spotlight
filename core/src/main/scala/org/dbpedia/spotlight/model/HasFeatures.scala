package org.dbpedia.spotlight.model

import collection.mutable.HashMap

/**
 * @author Joachim Daiber
 */

trait HasFeatures {

  val features = HashMap[String, Feature]()

  def feature[T](feature: String): Option[T] = {
    features.get(feature) match {
      case Some(f) => Option(f.value.asInstanceOf[T])
      case None => None
    }
  }

  def setFeature(feature: Feature) {
    features.put(feature.featureName, feature)
  }

}
