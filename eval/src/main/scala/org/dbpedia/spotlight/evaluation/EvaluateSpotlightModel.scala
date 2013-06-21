package org.dbpedia.spotlight.evaluation

import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.io.WikipediaHeldoutCorpus
import org.dbpedia.spotlight.db._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.Paragraph
import scala.io.Source
import org.dbpedia.spotlight.db.concurrent.SpotterWrapper

object EvaluateSpotlightModel {

  def main(args: Array[String]) {

    val model = SpotlightModel.fromFolder(new File(args(0)))
    val (_, sfStore, resStore, candMapStore, _) = SpotlightModel.storesFromFolder(new File(args(0)))

    val memLoaded = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
    System.err.println("Memory footprint (model loaded): %s".format( memLoaded ) )

    val heldout = new File(args(1), "heldout.txt")

    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      model.properties.getProperty("namespace"),
      new FileInputStream(new File(args(1), "redirects.nt")),
      new FileInputStream(new File(args(1), "disambiguations.nt"))
    )


    val spotter = model.spotters.get(SpotterPolicy.Default)
    val disambiguator = model.disambiguators.get(DisambiguationPolicy.Default)

    val corpusDisambiguate = new WikipediaHeldoutCorpus(Source.fromFile(heldout).getLines().toSeq.take(6000), Option(wikipediaToDBpediaClosure), Option(disambiguator.disambiguator.asInstanceOf[DBTwoStepDisambiguator].candidateSearcher))
    val memInit = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
    System.err.println("Memory footprint (corpus): %s".format( memInit ) )


    //Time performance:
    val startTime = System.nanoTime()
    corpusDisambiguate.foreach(p => {
      val text = p.text
      model.tokenizer.tokenizeMaybe(text)
      val spots = spotter.extract(text)
      disambiguator.disambiguate(new Paragraph("", text, spots.toList))
    })
    val endTime = System.nanoTime()
    val t = (endTime-startTime) / 1000000000
    System.err.println("Annotation time: %s sec".format( t ))
    System.err.println("Annotation time avg: %s sec".format( t / corpusDisambiguate.size.toDouble) )

    val memFinal = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
    System.err.println("Memory footprint: %s".format( memFinal ) )

    //Set tokenizer:
    spotter match {
      case spotter: DBSpotter => spotter.tokenizer = model.tokenizer
      case spotterW: SpotterWrapper => spotterW.spotters.foreach(_.asInstanceOf[DBSpotter].tokenizer = model.tokenizer)
    }
    disambiguator.disambiguator.asInstanceOf[DBTwoStepDisambiguator].tokenizer = model.tokenizer

    val baseline: DBBaselineDisambiguator = new DBBaselineDisambiguator(sfStore, resStore, candMapStore)

    //Evaluate full:
    EvaluateParagraphDisambiguator.evaluate(corpusDisambiguate, disambiguator.disambiguator, List(), List())

    //Evaluate baseline:
    EvaluateParagraphDisambiguator.evaluate(corpusDisambiguate, baseline, List(), List())

    //Spotting:
    val corpusSpot = new WikipediaHeldoutCorpus(Source.fromFile(heldout).getLines().toSeq.take(6000), None, None)
    val expected = EvalSpotter.getExpectedResult(corpusSpot)
    EvalSpotter.evalSpotter(corpusDisambiguate, spotter, expected)


  }
}
