package org.dbpedia.spotlight.spot


import org.dbpedia.spotlight.model.SurfaceFormOccurrence
import scalaj.collection.Imports._

class CapitalizedSelector extends SpotSelector {

  def select(occurrences: java.util.List[SurfaceFormOccurrence]) : java.util.List[SurfaceFormOccurrence] =
    occurrences.asScala.filter( _.surfaceForm.name.head.isUpper ).asJava

}
