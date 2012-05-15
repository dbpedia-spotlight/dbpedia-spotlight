package org.dbpedia.spotlight.spot.spotters

import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.model.{OntologyType, SurfaceFormOccurrence, Text, RequiresAnalyzedText}
import java.io.InputStream
import opennlp.tools.namefind.{TokenNameFinderModel, NameFinderME, TokenNameFinder}

/**
 * @author Joachim Daiber
 */

class OpenNLPNESpotter(
  val models: List[Pair[TokenNameFinder, OntologyType]]
) extends Spotter with RequiresAnalyzedText {

  def this(models: List[Pair[InputStream, OntologyType]]) {
    this(models.map { p: Pair[InputStream, OntologyType] =>
      Pair[TokenNameFinder, OntologyType](
        new NameFinderME(new TokenNameFinderModel(p._1)),
        p._2
      )
    })
  }


  def extract(text: Text): java.util.List[SurfaceFormOccurrence] = {

  }

}
