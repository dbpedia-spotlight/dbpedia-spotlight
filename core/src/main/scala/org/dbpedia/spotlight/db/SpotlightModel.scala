package org.dbpedia.spotlight.db

import memory.MemoryStore
import model._
import org.tartarus.snowball.SnowballProgram
import opennlp.tools.tokenize.{TokenizerModel, TokenizerME}
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetectorME}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import org.dbpedia.spotlight.disambiguate.mixtures.UnweightedMixture
import similarity.GenerativeContextSimilarity
import org.dbpedia.spotlight.spot.opennlp.OpenNLPChunkerSpotterDB
import scala.collection.JavaConverters._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ
import org.dbpedia.spotlight.spot.Spotter
import java.io.{File, FileInputStream}
import java.util.Properties
import breeze.linalg.DenseVector

class SpotlightModel(val tokenizer: Tokenizer,
                     val spotters: java.util.Map[SpotterPolicy, Spotter],
                     val disambiguators: java.util.Map[DisambiguationPolicy, ParagraphDisambiguatorJ],
                     val properties: Properties)

object SpotlightModel {

  def loadStopwords(modelFolder: File): Set[String] = scala.io.Source.fromFile(new File(modelFolder, "stopwords.list")).getLines().map(_.trim()).toSet
  def loadSpotterThresholds(file: File): Seq[Double] = scala.io.Source.fromFile(file).getLines().next().split(" ").map(_.toDouble)

  def fromFolder(modelFolder: File): SpotlightModel = {

    val modelDataFolder = new File(modelFolder, "model")
    val tokenTypeStore = MemoryStore.loadTokenTypeStore(new FileInputStream(new File(modelDataFolder, "tokens.mem")))
    val sfStore =        MemoryStore.loadSurfaceFormStore(new FileInputStream(new File(modelDataFolder, "sf.mem")))
    val resStore =       MemoryStore.loadResourceStore(new FileInputStream(new File(modelDataFolder, "res.mem")))
    val candMapStore =   MemoryStore.loadCandidateMapStore(new FileInputStream(new File(modelDataFolder, "candmap.mem")), resStore)
    val contextStore =   MemoryStore.loadContextStore(new FileInputStream(new File(modelDataFolder, "context.mem")), tokenTypeStore)

    val stopwords = loadStopwords(modelFolder)

    val properties = new Properties()
    properties.load(new FileInputStream(new File(modelFolder, "model.properties")))

    //Load the stemmer from the model file:
    val stemmer: SnowballProgram = properties.getProperty("stemmer") match {
      case s: String if s equals "None" => null
      case s: String => Class.forName("org.tartarus.snowball.ext.%s".format(s)).newInstance().asInstanceOf[SnowballProgram]
    }

    //Create the tokenizer:
    val tokenizer: Tokenizer = new DefaultTokenizer(
      new TokenizerME(new TokenizerModel(new FileInputStream(new File(modelFolder, "opennlp/token.bin")))),
      stopwords,
      stemmer,
      new SentenceDetectorME(new SentenceModel(new FileInputStream(new File(modelFolder, "opennlp/sent.bin")))),
      new POSTaggerME(new POSModel(new FileInputStream(new File(modelFolder, "opennlp/pos-maxent.bin")))),
      tokenTypeStore
    )

    val searcher      = new DBCandidateSearcher(resStore, sfStore, candMapStore)
    val disambiguator = new ParagraphDisambiguatorJ(new DBTwoStepDisambiguator(
      tokenTypeStore,
      sfStore,
      resStore,
      searcher,
      contextStore,
      new UnweightedMixture(Set("P(e)", "P(c|e)", "P(s|e)")),
      new GenerativeContextSimilarity(tokenTypeStore)
    ))

    val spotter = new OpenNLPChunkerSpotterDB(
      new FileInputStream(new File(modelFolder, "opennlp/chunker.bin")),
      sfStore,
      stopwords,
      Some(loadSpotterThresholds(new File(modelFolder, "opennlp_chunker_thresholds.txt"))),
      Set("NP", "MWU", "PP"), "N"
    ).asInstanceOf[Spotter]

    val spotters: java.util.Map[SpotterPolicy, Spotter] = Map(SpotterPolicy.Default -> spotter).asJava
    val disambiguators: java.util.Map[DisambiguationPolicy, ParagraphDisambiguatorJ] = Map(DisambiguationPolicy.Default -> disambiguator).asJava

    new SpotlightModel(tokenizer, spotters, disambiguators, properties)
  }
}
