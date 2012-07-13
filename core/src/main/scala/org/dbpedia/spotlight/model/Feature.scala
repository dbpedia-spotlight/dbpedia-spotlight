package org.dbpedia.spotlight.model

/**
 * Generic class for specifying features of occurrences.
 * Getting values can use pattern matching.
 *
 */
//class Feature[T<:Any](val featureName: String, val value: T)
class Feature(val featureName: String, val value: Any) {

    override def toString : String = {
        this match {
            case s : Score => s.value.toString
            case n : Nominal => n.value.toString
            case _ => value.toString
        }
    }
}

class Score(name: String, value: Double) extends Feature(name,value)

class Nominal(name: String, value: String) extends Feature(name,value)