package org.dbpedia.spotlight.spot

import org.dbpedia.spotlight.model.SurfaceFormOccurrence
import scala.collection.JavaConversions._
/**
 * We have observed that almost every letter or combination of two letters seems to have a Wikipedia page.
 * Therefore
 * @author pablomendes
 */
class ShortSurfaceFormSelector(val minLength: Int=3) extends SpotSelector {

    def select(occs: java.util.List[SurfaceFormOccurrence]) : java.util.List[SurfaceFormOccurrence] = {
        occs.filterNot(o => o.surfaceForm.name.length<minLength)
    }

}
