package org.dbpedia.spotlight.evaluation

import java.io.File
import org.dbpedia.spotlight.io.WikipediaHeldoutCorpus
import org.dbpedia.spotlight.db.SpotlightModel
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy

class EvaluateSpotlightModel {

  def main(args: Array[String]) {

    val model = SpotlightModel.fromFolder(args(1))
    val heldout = new File(args(2))

    val corpus = WikipediaHeldoutCorpus.fromFile(heldout)

    val spotter = model.spotters.get(SpotterPolicy.Default)
    val disambiguator = model.disambiguators.get(DisambiguationPolicy.Default)

    //Spotting:
    val expected = EvalSpotter.getExpectedResult(corpus)
    EvalSpotter.evalSpotting(corpus, spotter, expected)

    //Disambiguation
    EvaluateParagraphDisambiguator.evaluate(corpus, disambiguator, List(), List())

  }

}
