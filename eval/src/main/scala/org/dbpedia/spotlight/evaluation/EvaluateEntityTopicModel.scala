package org.dbpedia.spotlight.evaluation


import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.db.DBTwoStepDisambiguator
import org.dbpedia.spotlight.io.{WikiPageUtil, WikiOccurrenceSource, WikiPageContextSource, WikipediaHeldoutCorpus}
import org.dbpedia.spotlight.db._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, Paragraph}
import scala.io.Source
import scala.io.Codec
import org.dbpedia.spotlight.db.concurrent.SpotterWrapper
import org.dbpedia.spotlight.db.entitytopic.EntityTopicDisambiguator
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.{WikiPage, XMLSource}
import org.dbpedia.spotlight.string.WikiMarkupStripper
import org.dbpedia.extraction.wikiparser.{WikiParser, NodeUtil}
import scala.collection.mutable.ListBuffer


object EvaluateEntityTopicModel {


  def main(args: Array[String]) {
    val heldout = new File(args(0))
    val model = EntityTopicDisambiguator.fromFolder(new File(args(1)),args(2).toInt)

    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      model.properties.getProperty("namespace"),
      new FileInputStream(new File(args(2), "redirects.nt")),
      new FileInputStream(new File(args(2), "disambiguations.nt"))
    )

    val disambiguator = model
    val lines=Source.fromInputStream(new FileInputStream(heldout))(Codec("UTF-8")).getLines().toSeq.take(1000)
    val corpusDisambiguate = new WikipediaHeldoutCorpus(lines, Option(wikipediaToDBpediaClosure), Option(disambiguator.searcher))
    val memInit = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
    System.err.println("Memory footprint (corpus): %s".format( memInit ) )


    //Evaluate entitytopic disambiguator:
    EvaluateParagraphDisambiguator.evaluate(corpusDisambiguate, disambiguator, List(), List())

    //val baseline: DBBaselineDisambiguator = new DBBaselineDisambiguator(sfStore, resStore, candMapStore)
    //Evaluate baseline:
    //EvaluateParagraphDisambiguator.evaluate(corpusDisambiguate, baseline, List(), List())

  }
}
