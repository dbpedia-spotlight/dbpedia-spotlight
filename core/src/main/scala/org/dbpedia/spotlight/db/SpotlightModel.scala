package org.dbpedia.spotlight.db

import java.util.Properties

import org.apache.commons.logging.{LogFactory, Log}
import org.dbpedia.spotlight.db.model.TextTokenizer
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ
import org.dbpedia.spotlight.exceptions.InputException
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.sparql.SparqlQueryExecuter
import org.dbpedia.spotlight.spot.Spotter

/**
 * Created by David Przybilla on 19/08/2014.
 */


trait SpotlightModel{

  val tokenizer: TextTokenizer
  val spotters: java.util.Map[SpotterPolicy, Spotter]
  val disambiguators: java.util.Map[DisambiguationPolicy, ParagraphDisambiguatorJ]
  val properties: Properties

  val LOG: Log = LogFactory.getLog(this.getClass)

  def spot(text: Text, params: AnnotationParameters): java.util.List[SurfaceFormOccurrence];

  @throws( classOf[InputException] )
  def firstBest(text: String, params: AnnotationParameters): java.util.List[DBpediaResourceOccurrence] ;

  def getSpotter(name: String): Spotter = {
    var policy: SpotterConfiguration.SpotterPolicy = SpotterPolicy.Default
    try {
      policy = SpotterPolicy.valueOf(name)
    }
    catch {
      case e: IllegalArgumentException => {
        throw new InputException(String.format("Specified parameter spotter=%s is invalid. Use one of %s.", name, SpotterPolicy.values))
      }
    }
    if (spotters.size == 0) throw new InputException(String.format("No spotters were loaded. Please add one of %s.", spotters.keySet))
    val spotter: Spotter = spotters.get(policy)
    if (spotter == null) {
      throw new InputException(String.format("Specified spotter=%s has not been loaded. Use one of %s.", name, spotters.keySet))
    }
    return spotter
  }

  //def firstBest(text: String, params: AnnotationParameters): java.util.List[DBpediaResourceOccurrence] ;
  //def nBest(text: String, params: AnnotationParameters ):  java.util.List[DBpediaResourceOccurrence];


}
