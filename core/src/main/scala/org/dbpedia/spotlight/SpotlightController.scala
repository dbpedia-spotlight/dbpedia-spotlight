package org.dbpedia.spotlight

import annotate.DefaultAnnotator
import lucene.disambiguate.MergedOccurrencesDisambiguator
import lucene.LuceneManager
import lucene.search.MergedOccurrencesContextSearcher
import model.Factory
import spot._
import dictionary.ExactSurfaceFormDictionary
import selectors._
import scala.xml.{Node, XML}

import java.io._
import org.apache.commons.logging.LogFactory
import spotters.{OpenNLPNESpotter, OpenNLPChunkerSpotter, NESpotter, LingPipeSpotter}
import util.ConfigUtil._
import scala.collection.JavaConversions._
import org.apache.lucene.store.Directory

/**
 * @author Joachim Daiber
 */

class SpotlightController(xmlConfiguration: Node) {

  private val log = LogFactory.getLog(this.getClass)

  /**
   * Load the configuration from the specified file.
   *
   * @param configurationFile XML configuration file
   */
  def this(configurationFile: File) {
    this(XML.loadFile(configurationFile))
  }


  /**
   * Load the default toy configuration.
   */
  def this() {
    this(XML.load("data/etc/dbpedia-spotlight/server.xml"))
  }


  //Load the default values for any configuration:
  private val defaultConf = XML.load(
    this.getClass.getClassLoader.getResourceAsStream("conf/server.default.xml")
  )

  //Load and check the real configuration:
  private val conf = (xmlConfiguration \ "configuration").head
  sanityCheck(conf)


  /*******************************************************************
   * General settings                                                *
   *******************************************************************/

  val serverURI     = globalParameter[String]("settings/rest_uri")
  val language      = globalParameter[String]("settings/language")
  val stopwordsFile = globalParameter[InputStream]("settings/stopword_file")


  /*******************************************************************
   * Initialize the components                                       *
   *******************************************************************/

  private val spotters = ((conf \ "spotters") map( sp =>
    Pair((sp \ "@id").text, buildSpotter(sp)))).toMap

  private val spotSelectors = (conf \ "spot_selectors" map( spsl =>
    Pair((spsl \ "@id").text, buildSpotSelector(spsl)))).toMap

  private val spotterByPolicy = (conf \ "spotting_policies" map( sp =>
    Pair((sp \ "@id").text, buildSpottingPolicy(sp)))).toMap

  private val defaultSpottingPolicy = (((conf \ "spotting_policies").head) \ "@id").text


  /*******************************************************************
   * Component access methods                                        *
   *******************************************************************/

  //Spotting:
  def spotter(spotterPolicy: String): Spotter = {
    spotterByPolicy.getOrElse(spotterPolicy, spotterByPolicy.get(defaultSpottingPolicy).get)
  }
  def spotter(): Spotter = spotter(defaultSpottingPolicy)

  //Disambiguation:
  val directory: Directory = LuceneManager.pickDirectory(new File(configuration.getContextIndexDirectory))
  val luceneManager: LuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
  val searcher = new MergedOccurrencesContextSearcher(luceneManager);

  //Annotation:
  def annotator() ={
    //new MergedOccurrencesDisambiguator(searcher)
    new DefaultAnnotator(spotter(), disambiguator())
  }



  /*******************************************************************
   *  Builder methods                                                *
   *******************************************************************/

  private def buildSpotter(spotter: Node): Spotter = {
    log.info( "Building spotter %s".format(spotter \ "@id") )
    (spotter \ "@id").text match {

      case "LingPipeSpotter" => new LingPipeSpotter(
        localParameter[InputStream]("dictionary", spotter)
      )

      case "LexicalizedNPSpotter" => new OpenNLPChunkerSpotter(
        localParameter[InputStream]("chunker_model", spotter),
        ExactSurfaceFormDictionary.fromInputStream( localParameter[InputStream]("dictionary", spotter) ),
        stopwordsFile,
        localParameter[String]("np_tag", spotter),
        localParameter[String]("nn_tag", spotter)
      )

      case "NESpotter" =>
        new OpenNLPNESpotter(
          (spotter \ "no_model").map( ne_model =>
            Pair(localParameter[InputStream]("model_file", ne_model),
              Factory.OntologyType.fromQName(localParameter[String]("model_type", ne_model)))
          ).toList
        )

    }
  }

  private def buildSpotSelector(spotSelector: Node): SpotSelector = {
    log.info( "Building spott selector %s".format(spotSelector \ "@id") )
    (spotSelector \ "@id").text match {

      case "NP*Selector" =>  new AtLeastOneNounSelector()

      case "CapitalizedSelector" => new CapitalizedSelector()

      case "WhitelistSelector" => new WhitelistSelector(
        localParameter[InputStream]("dictionary", spotSelector)
      )

      case "CoOccurrenceBasedSelector" => new CoOccurrenceBasedSelector(
        localParameter[String]("database/jdbc_driver", spotSelector),
        localParameter[String]("database/connector", spotSelector),
        localParameter[String]("database/user", spotSelector),
        localParameter[String]("database/password", spotSelector),
        localParameter[InputStream]("model_unigram", spotSelector),
        localParameter[InputStream]("ngram", spotSelector),
        localParameter[String]("datasource", spotSelector)
      )
    }
  }

  private def buildSpottingPolicy(spotterPolicy: Node): Spotter = {
    log.info( "Building spotting policy %s".format(spotterPolicy \ "@id") )
    val pSpotter = spotters((((spotterPolicy \ "spotter").head) \ "@ref").text)
    val pSelectors =
      ((spotterPolicy \ "spot_selector") map (ss => spotSelectors((ss \ "@ref").text))).toList

    if (spotSelectors.size == 0) {
      pSpotter
    } else {
      new SpotterWithSelectors(pSpotter, pSelectors)
    }
  }


  /*******************************************************************
   * Utils for reading the configuration                             *
   *******************************************************************/

  def globalParameter[T](path: String): T =
    parameter[T](path.split("/"), conf, defaultConf)

  def localParameter[T](path: String, base: Node): T =
    parameter[T](path.split("/"), base, defaultNode(base, defaultConf))

}