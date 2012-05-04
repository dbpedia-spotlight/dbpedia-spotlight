package org.dbpedia.spotlight.spot

import java.util.List
import org.dbpedia.spotlight.model.SurfaceFormOccurrence
import scala.collection.JavaConversions._

/**
 * @author Joachim Daiber
 *
 */

abstract class SelectorPolicy {

  /**
   * Combine the spots from a Spotter with the selections of one or more spot
   * selectors.
   *
   * @param spots the spots from the Spotter
   * @param selections the selections from the SpotSelector
   * @return
   */
  def combine(spots: List[SurfaceFormOccurrence], selections: List[List[SurfaceFormOccurrence]]): List[SurfaceFormOccurrence]
}

object SelectorPolicy {

  val intersection = new SelectorPolicy {

    /**
     * The resulting spots will be the intersection of the Spotter and all
     * SpotSelectors.
     *
     * @param spots the spots from the Spotter
     * @param selections the selections from the SpotSelector
     * @return
     */
    def combine(spots: List[SurfaceFormOccurrence], selections: List[List[SurfaceFormOccurrence]]): List[SurfaceFormOccurrence] = {
      var spotSet = spots.toSet
      selections.foreach { selection =>
        spotSet = spotSet.intersect(selection)
      }
      spotSet
    }
  }

  val union = new SelectorPolicy {

    /**
     * The resulting spots will be the union of the Spotter and all
     * SpotSelectors.
     *
     * @param spots the spots from the Spotter
     * @param selections the selections from the SpotSelector
     * @return
     */
    def combine(spots: List[SurfaceFormOccurrence], selections: List[List[SurfaceFormOccurrence]]): List[SurfaceFormOccurrence] = {
      var allSelections = Set[SurfaceFormOccurrence]()
      selections.foreach { selection =>
        allSelections = selection.union(selection)
      }
      spots.toSet.intersection(allSelections)
    }
  }


}