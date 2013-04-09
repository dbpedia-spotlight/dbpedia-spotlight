package org.dbpedia.spotlight.evaluation

import java.io.File
import org.dbpedia.spotlight.io.WikipediaHeldoutCorpus
import org.dbpedia.spotlight.db.{DBTwoStepDisambiguator, DBSpotter, SpotlightModel}
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.Paragraph

object EvaluateSpotlightModel {

  def main(args: Array[String]) {

    val model = SpotlightModel.fromFolder(new File(args(0)))

    val memLoaded = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
    println("Memory footprint (model loaded): %s".format( memLoaded ) )

    val heldout = new File(args(1))
    val corpus = WikipediaHeldoutCorpus.fromFile(heldout)
    val memInit = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
    println("Memory footprint (corpus): %s".format( memInit ) )


    val spotter = model.spotters.get(SpotterPolicy.Default)
    val disambiguator = model.disambiguators.get(DisambiguationPolicy.Default)

    //Time performance:
    val startTime = System.nanoTime()
    corpus.foreach(p => {
      val text = p.text
      model.tokenizer.tokenizeMaybe(text)
      val spots = spotter.extract(text)
      disambiguator.disambiguate(new Paragraph("", text, spots.toList))
    })
    val endTime = System.nanoTime()
    val t = (endTime-startTime) / 1000000000
    println("Annotation time: %s sec".format( t ))
    println("Annotation time avg: %s sec".format( t / corpus.size.toDouble) )

    val memFinal = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
    println("Memory footprint: %s".format( memFinal ) )

    //Spotting:
    val expected = EvalSpotter.getExpectedResult(corpus)

    //Set tokenizer:
    spotter.asInstanceOf[DBSpotter].tokenizer = model.tokenizer
    disambiguator.disambiguator.asInstanceOf[DBTwoStepDisambiguator].tokenizer = model.tokenizer

    EvalSpotter.evalSpotter(corpus, spotter, expected)
    EvaluateParagraphDisambiguator.evaluate(corpus, disambiguator.disambiguator, List(), List())

  }
}
