package org.dbpedia.spotlight.spot

import org.dbpedia.spotlight.model.{SurfaceFormOccurrence, Text}
import org.apache.commons.logging.LogFactory
import java.util.List
import scala.collection.JavaConversions._

/**
 * Applies the Spotter and filters the resulting spots using the spot selectors.
 *
 * @author Joachim Daiber
 *
 * TODO add selector policies (union, intersection, etc.)
 *
 */

class SpotterWithSelectors(val spotter: Spotter, val spotSelectors: List[SpotSelector], val selectorPolicy: SelectorPolicy) extends Spotter {

  private val LOG = LogFactory.getLog(this.getClass)

  /**
   * Use the default selector policy (intersection).
   *
   * @param spotter
   * @param spotSelectors
   */
  def this(spotter: Spotter, spotSelectors: List[SpotSelector]) {
    this(spotter, spotSelectors, SelectorPolicy.intersection)
  }

  /**
   * Applies the base spotter specified, then applies the selectors on the generated spots.
   *
   * @param text
   * @return
   * @throws SpottingException
   */
  override def extract(text: Text): List[SurfaceFormOccurrence] = {

    LOG.debug(String.format("Spotting with spotter %s and selectors %s.", spotter.getName, spotSelectors))
    var spots: List[SurfaceFormOccurrence] = spotter.extract(text)
    selectorPolicy.combine(spots, spotSelectors.map( select(_, spots) ) )
  }


  private def select(spotSelector: SpotSelector, spottedSurfaceForms: List[SurfaceFormOccurrence]): List[SurfaceFormOccurrence] = {

    val selectedSpots = spotSelector.select(spottedSurfaceForms)
    LOG.info("Selecting candidates...")
    val count: Int = spottedSurfaceForms.size - selectedSpots.size
    val percent: String = if ((count == 0)) "0" else "%1.0f".format((count / spottedSurfaceForms.size) * 100)
    LOG.info( "Removed %s (%s percent) spots using spotSelector %s".format(count, percent, spotSelector.getClass.getSimpleName))

    selectedSpots
  }

}
