package org.dbpedia.spotlight.model

/**
 * Generic class for specifying features of occurrences.
 * Getting values can use pattern matching.
 *
 */
//class Feature[T<:Any](val featureName: String, val value: T)
class Feature(val featureName: String, val value: Any)

class Score(name: String, score: Double) extends Feature(name,score)