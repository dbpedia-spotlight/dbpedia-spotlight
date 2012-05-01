package org.dbpedia.spotlight.spot.spotters

import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.model.{SurfaceFormOccurrence, Text, RequiresAnalyzedText}

/**
 * @author Joachim Daiber
 */

class OpenNLPNESpotter(

) extends Spotter with RequiresAnalyzedText {

  def extract(text: Text): java.util.List[SurfaceFormOccurrence] = {

  }

}
