package org.dbpedia.spotlight.evaluation


import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.db.DBTwoStepDisambiguator
import org.dbpedia.spotlight.io.WikipediaHeldoutCorpus
import org.dbpedia.spotlight.db._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.Paragraph
import scala.io.Source
import org.dbpedia.spotlight.db.concurrent.SpotterWrapper
import org.dbpedia.spotlight.db.entitytopic.EntityTopicModel


object EvaluateEntityTopicModel {


  def main(args: Array[String]) {
    val heldout = new File(args(0))
    val model = EntityTopicModel.fromFolder(new File(args(1)),args(2),args(3).toInt)
    //val (_, sfStore, resStore, candMapStore, _) = SpotlightModel.storesFromFolder(new File(args(0)))

    val memLoaded = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
    System.err.println("Memory footprint (model loaded): %s".format( memLoaded ) )



    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      model.properties.getProperty("namespace"),
      new FileInputStream(new File(args(2), "redirects.nt")),
      new FileInputStream(new File(args(2), "disambiguations.nt"))
    )

    val disambiguator = model
    val lines=Source.fromInputStream(new FileInputStream(heldout))(io.Codec("UTF-8")).getLines().toSeq.take(1000)
    val corpusDisambiguate = new WikipediaHeldoutCorpus(lines, Option(wikipediaToDBpediaClosure), Option(disambiguator.searcher))
    val memInit = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
    System.err.println("Memory footprint (corpus): %s".format( memInit ) )


    //disambiguator.disambiguator.asInstanceOf[DBTwoStepDisambiguator].tokenizer = model.tokenizer

    //val baseline: DBBaselineDisambiguator = new DBBaselineDisambiguator(model.searcher.sfStore, model.searcher.resStore, model)

    //Evaluate full:
    EvaluateParagraphDisambiguator.evaluate(corpusDisambiguate, disambiguator, List(), List())

    //Evaluate baseline:
    //EvaluateParagraphDisambiguator.evaluate(corpusDisambiguate, baseline, List(), List())

  }
}
