package org.dbpedia.spotlight.spot.selectors

import org.dbpedia.spotlight.model.SurfaceFormOccurrence
import scalaj.collection.Imports._
import org.dbpedia.spotlight.spot.SpotSelector

class CapitalizedSelector extends SpotSelector {

  def select(occurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[SurfaceFormOccurrence] =
    occurrences.asScala.filter(_.surfaceForm.name.head.isUpper).asJava

}
