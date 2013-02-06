package org.dbpedia.spotlight.db

import concurrent.{TokenizerWrapper, SpotterWrapper}
import memory.MemoryStore
import model._
import org.tartarus.snowball.SnowballProgram
import opennlp.tools.tokenize.{TokenizerModel, TokenizerME}
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetectorME}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import org.dbpedia.spotlight.disambiguate.mixtures.UnweightedMixture
import similarity.GenerativeContextSimilarity
import org.dbpedia.spotlight.spot.opennlp.OpenNLPSpotter
import scala.collection.JavaConverters._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ
import org.dbpedia.spotlight.spot.Spotter
import java.io.{File, FileInputStream}
import java.util.Properties
import breeze.linalg.DenseVector
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.namefind.TokenNameFinderModel

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
    val posTagger = new File(modelFolder, "opennlp/pos-maxent.bin")
    val posModel  = new POSModel(new FileInputStream(posTagger))
    val tokenModel = new TokenizerModel(new FileInputStream(new File(modelFolder, "opennlp/token.bin")))
    val sentenceModel = new SentenceModel(new FileInputStream(new File(modelFolder, "opennlp/sent.bin")))

    val cores = (1 to Runtime.getRuntime.availableProcessors())


    val tokenizer: Tokenizer = new TokenizerWrapper(
      cores.map(_ =>
        new DefaultTokenizer(
          new TokenizerME(tokenModel),
          stopwords,
          stemmer,
          new SentenceDetectorME(sentenceModel),
          if (posTagger.exists()) new POSTaggerME(posModel) else null,
          tokenTypeStore
        )
      )
    ).asInstanceOf[Tokenizer]

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

    val nerModels = new File(modelFolder, "opennlp").list().filter(_.startsWith("ner-")).map { f: String =>
      new TokenNameFinderModel(new FileInputStream(new File(new File(modelFolder, "opennlp"), f)))
    }.toList

    val chunkerFile = new File(modelFolder, "opennlp/chunker.bin")
    val chunkerModel = if (chunkerFile.exists())
      Some(new ChunkerModel(new FileInputStream(chunkerFile)))
    else
      None

    val spotter = new SpotterWrapper(
      cores.map(_ =>
        new OpenNLPSpotter(
          chunkerModel,
          nerModels,
          sfStore,
          stopwords,
          Some(loadSpotterThresholds(new File(modelFolder, "opennlp_chunker_thresholds.txt"))),
          Set("NP", "MWU", "PP"), "N"
        ).asInstanceOf[Spotter]
      )
    ).asInstanceOf[Spotter]

    val spotters: java.util.Map[SpotterPolicy, Spotter] = Map(SpotterPolicy.Default -> spotter).asJava
    val disambiguators: java.util.Map[DisambiguationPolicy, ParagraphDisambiguatorJ] = Map(DisambiguationPolicy.Default -> disambiguator).asJava

    new SpotlightModel(tokenizer, spotters, disambiguators, properties)
  }
}
