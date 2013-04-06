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
import scala.collection.JavaConverters._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ
import org.dbpedia.spotlight.spot.{SpotXmlParser, Spotter}
import java.io.{File, FileInputStream}
import java.util.{Locale, Properties}
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.namefind.TokenNameFinderModel
import stem.SnowballStemmer
import tokenize.{OpenNLPTokenizer, LanguageIndependentTokenizer, BaseTextTokenizer}


class SpotlightModel(val tokenizer: TextTokenizer,
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
    val contextStore = if (new File(modelDataFolder, "context.mem").exists())
      MemoryStore.loadContextStore(new FileInputStream(new File(modelDataFolder, "context.mem")), tokenTypeStore)
    else
      null

    val stopwords = loadStopwords(modelFolder)

    val properties = new Properties()
    properties.load(new FileInputStream(new File(modelFolder, "model.properties")))

    //Load the stemmer from the model file:
    def stemmer(): Stemmer = properties.getProperty("stemmer") match {
      case s: String if s equals "None" => null
      case s: String => new SnowballStemmer(s)
    }

    val c = properties.getProperty("opennlp_parallel", Runtime.getRuntime.availableProcessors().toString).toInt
    val cores = (1 to c)

    val tokenizer: TextTokenizer = if(new File(modelFolder, "opennlp").exists()) {

      //Create the tokenizer:
      val posTagger = new File(modelFolder, "opennlp/pos-maxent.bin")
      val posModel  = new POSModel(new FileInputStream(posTagger))
      val tokenizerModel = new TokenizerModel(new FileInputStream(new File(modelFolder, "opennlp/token.bin")))
      val sentenceModel = new SentenceModel(new FileInputStream(new File(modelFolder, "opennlp/sent.bin")))

      def createTokenizer() = new OpenNLPTokenizer(
        new TokenizerME(tokenizerModel),
        stopwords,
        stemmer(),
        new SentenceDetectorME(sentenceModel),
        if (posTagger.exists()) new POSTaggerME(posModel) else null,
        tokenTypeStore
      ).asInstanceOf[TextTokenizer]

      if(cores.size == 1)
        createTokenizer()
      else
        new TokenizerWrapper(cores.map(_ => createTokenizer())).asInstanceOf[TextTokenizer]

    } else {
      val locale = properties.getProperty("locale").split("_")
      new LanguageIndependentTokenizer(stopwords, stemmer(), new Locale(locale(0), locale(1)), tokenTypeStore)
    }

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

    val spotter = if(new File(modelFolder, "opennlp/pos-maxent.bin").exists()) {
      val nerModels = new File(modelFolder, "opennlp").list().filter(_.startsWith("ner-")).map { f: String =>
        new TokenNameFinderModel(new FileInputStream(new File(new File(modelFolder, "opennlp"), f)))
      }.toList

      val chunkerFile = new File(modelFolder, "opennlp/chunker.bin")
      val chunkerModel = if (chunkerFile.exists())
        Some(new ChunkerModel(new FileInputStream(chunkerFile)))
      else
        None

      def createSpotter() = new OpenNLPSpotter(
        chunkerModel,
        nerModels,
        sfStore,
        stopwords,
        Some(loadSpotterThresholds(new File(modelFolder, "spotter_thresholds.txt")))
      ).asInstanceOf[Spotter]

      if(cores.size == 1)
        createSpotter()
      else
        new SpotterWrapper(
          cores.map(_ => createSpotter())
        ).asInstanceOf[Spotter]

    } else {
      val dict = MemoryStore.loadFSADictionary(new FileInputStream(new File(modelFolder, "fsa_dict.mem")))

      new FSASpotter(
        dict,
        sfStore,
        Some(loadSpotterThresholds(new File(modelFolder, "spotter_thresholds.txt"))),
        stopwords
      ).asInstanceOf[Spotter]
    }


    val spotters: java.util.Map[SpotterPolicy, Spotter] = Map(SpotterPolicy.SpotXmlParser -> new SpotXmlParser(), SpotterPolicy.Default -> spotter).asJava
    val disambiguators: java.util.Map[DisambiguationPolicy, ParagraphDisambiguatorJ] = Map(DisambiguationPolicy.Default -> disambiguator).asJava

    new SpotlightModel(tokenizer, spotters, disambiguators, properties)
  }
}
