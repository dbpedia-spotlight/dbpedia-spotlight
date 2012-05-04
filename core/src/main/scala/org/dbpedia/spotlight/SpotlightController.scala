package org.dbpedia.spotlight

import annotate.DefaultAnnotator
import lucene.disambiguate.MergedOccurrencesDisambiguator
import lucene.search.MergedOccurrencesContextSearcher
import spot._
import dictionary.ExactSurfaceFormDictionary
import selectors._
import scala.xml.{Node, XML}

import java.io._
import org.apache.commons.logging.LogFactory
import scala._
import spotters.{NESpotter, LingPipeSpotter, OpenNLPChunkerSpotter}
import util.ConfigUtil._
import scala.collection.JavaConversions._

/**
 * @author Joachim Daiber
 */

class SpotlightController(xmlConfiguration: Node) {

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


  private val log = LogFactory.getLog(this.getClass)

  //Load the default values for any configuration:
  private val defaultConf = XML.load(
    this.getClass.getClassLoader.getResourceAsStream("conf/server.default.xml")
  )

  //Real configuration:
  private val conf = (xmlConfiguration \ "configuration").head
  sanityCheck(conf)



  /*******************************************************************
   * General settings                                                *
   *******************************************************************/

  val serverURI     = globalParameter[String](List("settings", "rest_uri"))
  val language      = globalParameter[String](List("settings", "language"))
  val stopwordsFile = globalParameter[InputStream](List("settings", "stopword_file"))


  /*******************************************************************
   * Components                                                      *
   *******************************************************************/

  private val spotters = ((conf \ "spotters") map( spotter =>
    ((spotter \ "@id").text, buildSpotter(spotter))
    )).toMap


  private val spotSelectors = (conf \ "spot_selectors" map( spotSelector =>
    ((spotSelector \ "@id").text, buildSpotSelector(spotSelector))
    )).toMap


  private val spotterByPolicy = (conf \ "spotting_policies" map( spottingPolicy =>
    ((spottingPolicy \ "@id").text, buildSpottingPolicy(spottingPolicy))
    )).toMap
  private val defaultSpottingPolicy: String = (((conf \ "spotting_policies").head) \ "@id").text

  /**
   * Access to the spotters:
   */
  def spotter(spotterPolicy: String = "<default>"): Spotter = {
    spotterByPolicy.getOrElse(spotterPolicy, spotterByPolicy.get(defaultSpottingPolicy).get)
  }

  /**
   * Set searcher:
   */
  val searcher = new MergedOccurrencesContextSearcher(luceneManager);


  /**
   * Annotator
   */
  def annotator() ={
    new DefaultAnnotator(spotter(), new MergedOccurrencesDisambiguator(searcher))
  }



  /*******************************************************************
   *  Builder methods                                                *
   *******************************************************************/

  private def buildSpotter(spotter: Node): Spotter = {
    log.info( "Building spotter %s".format(spotter \ "@id") )
    (spotter \ "@id").text match {

      case "LingPipeSpotter" => new LingPipeSpotter(
        localParameter[InputStream](List("dictionary"), spotter)
      )

      case "LexicalizedNPSpotter" => new OpenNLPChunkerSpotter(
        localParameter[InputStream](List("chunker_model"), spotter),
        ExactSurfaceFormDictionary.fromInputStream( localParameter[InputStream](List("dictionary"), spotter) ),
        stopwordsFile,
        localParameter[String](List("np_tag"), spotter),
        localParameter[String](List("nn_tag"), spotter)
      )

      case "NESpotter" => new NESpotter(
        localParameter[InputStream](List("chunker_model"), spotter),
        ExactSurfaceFormDictionary.fromInputStream( localParameter[InputStream](List("dictionary"), spotter) ),
        stopwordsFile,
        localParameter[String](List("np_tag"), spotter),
        localParameter[String](List("nn_tag"), spotter)
      )

    }
  }

  private def buildSpotSelector(spotSelector: Node): SpotSelector = {
    log.info( "Building spott selector %s".format(spotSelector \ "@id") )
    (spotSelector \ "@id").text match {

      case "NP*Selector" =>  new AtLeastOneNounSelector()

      case "CapitalizedSelector" => new CapitalizedSelector()

      case "WhitelistSelector" => new WhitelistSelector(
        localParameter[InputStream](List("dictionary"), spotSelector)
      )

      case "CoOccurrenceBasedSelector" => new CoOccurrenceBasedSelector(
        localParameter[String](List("database", "jdbc_driver"), spotSelector),
        localParameter[String](List("database", "connector"), spotSelector),
        localParameter[String](List("database", "user"), spotSelector),
        localParameter[String](List("database", "password"), spotSelector),
        localParameter[InputStream](List("model_unigram"), spotSelector),
        localParameter[InputStream](List("ngram"), spotSelector),
        localParameter[String](List("datasource"), spotSelector)
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

  def globalParameter[T](path: List[String]): T =
    parameter[T](path, conf, defaultConf)

  def localParameter[T](path: List[String], base: Node): T =
    parameter[T](path, base, defaultNode(base, defaultConf))

}
