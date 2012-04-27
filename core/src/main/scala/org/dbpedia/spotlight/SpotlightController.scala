package org.dbpedia.spotlight

import annotate.DefaultAnnotator
import lucene.disambiguate.MergedOccurrencesDisambiguator
import lucene.search.MergedOccurrencesContextSearcher
import spot._
import spot.lingpipe.LingPipeSpotter
import spot.opennlp.{ExactSurfaceFormDictionary, OpenNLPChunkerSpotter}
import xml.{Node, XML}
import collection.immutable.HashMap
import java.io._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.util.ConfigUtil._


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
   * Load the default configuration.
   */
  def this() {
    this(defaultConfiguration)
  }


  private val log = LogFactory.getLog(this.getClass)

  //Default configuration:
  private val defaultConfiguration = XML.load(
    this.getClass.getClassLoader.getResourceAsStream(
      "deb/control/data/etc/dbpedia-spotlight/server.properties"
    )
  )

  //Real configuration:
  private val configuration = xmlConfiguration \ "configuration"
  sanityCheck(configuration)


  /**
   * Properties that will be used by other parts.
   */

  val serverURI     = (configuration \ "rest_uri").text
  val language      = (configuration \ "language").text
  val stopwordFile  = (configuration \ "stopword_file").text


  /**
   * Load Spotters:
   */
  private val spotters: HashMap[String, Spotter] = HashMap(
    configuration \ "spotters" foreach( spotter =>
      (spotter \ "@id", buildSpotter(spotter))
      )
  )

  private val spotSelectors: HashMap[String, Spotter] = HashMap(
    configuration \ "spot_selectors" foreach( spotSelector =>
      (spotSelector \ "@id", buildSpotSelector(spotSelector))
      )
  )

  private val spotterByPolicy: HashMap[String, Spotter] = HashMap(
    configuration \ "spotting_policies" foreach( spottingPolicy =>
      (spottingPolicy \ "@id", buildSpottingPolicy(spottingPolicy))
      )
  )

  private val defaultSpottingPolicy: String =
    (configuration \ "spotting_policies").filter( node => node.attribute("default").exists() ).head \ "@id"

  /**
   * Access to the spotters:
   */
  def spotter(): Spotter = spotterByPolicy(defaultSpottingPolicy)
  def spotter(spotterPolicy: String) = spotterByPolicy.getOrElse(spotterPolicy, spotter())

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

  private def buildSpotter(spotter: Node): Spotter = {
    log.info( "Building spotter %s".format(spotter \ "@id") )
    spotter \ "@id" match {

      case "LingPipeSpotter" => new LingPipeSpotter(
        file((spotter \ "dictionary").text)
      )

      case "LexicalizedNPSpotter" => new OpenNLPChunkerSpotter(
        file((spotter \ "chunker_model").text),
        new ExactSurfaceFormDictionary(
          file((spotter \ "dictionary").text)
        ),
        stopwordsFile
      )
    }
  }

  private def buildSpotSelector(spotSelector: Node): SpotSelector = {
    log.info( "Building spott selector %s".format(spotSelector \ "@id") )
    spotSelector \ "@id" match {

      case "NP*Selector" =>  new AtLeastOneNounSelector()

      case "CapitalizedSelector" => new CapitalizedSelector()

      case "WhitelistSelector" => new WhitelistSelector(
        file((spotter \ "dictionary").text)
      )

      case "CoOccurrenceBasedSelector" => //
    }
  }

  private def buildSpottingPolicy(spotterPolicy: Node): Spotter = {
    log.info( "Building spotting policy %s".format(spotSelector \ "@id") )
    val pSpotter = spotters(spotterPolicy \ "spotter" \ "@ref")
    val pSelectors: List[SpotSelector] = (spotterPolicy \\ "spot_selector" \ "@ref") foreach (spotSelectors(_))

    SpotterWithSelector.getInstance(
      pSpotter,
      if (pSelectors.size > 1) {
        new ChainedSelector(pSelectors)
      } else {
        pSelectors.head
      }
    )
  }
}
