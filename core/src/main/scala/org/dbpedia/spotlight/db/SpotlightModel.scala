package org.dbpedia.spotlight.db

import java.util

import concurrent.{TokenizerWrapper, SpotterWrapper}
import org.dbpedia.spotlight.db.memory.{MemoryVectorStore, MemoryContextStore, MemoryStore}
import model._
import opennlp.tools.tokenize.{TokenizerModel, TokenizerME}
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetectorME}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import org.dbpedia.spotlight.disambiguate.mixtures.{LogLinearFeatureMixture, LinearRegressionFeatureMixture, Mixture, UnweightedMixture}
import org.dbpedia.spotlight.db.similarity.{VectorContextSimilarity, NoContextSimilarity, GenerativeContextSimilarity, ContextSimilarity}
import org.dbpedia.spotlight.log.SpotlightLog
import scala.collection.JavaConverters._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ
import org.dbpedia.spotlight.spot.{SpotXmlParser, Spotter}
import java.io.{Reader, IOException, File, FileInputStream}
import java.util.{Locale, Properties}
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.namefind.TokenNameFinderModel
import stem.SnowballStemmer
import tokenize.{OpenNLPTokenizer, LanguageIndependentTokenizer}
import org.dbpedia.spotlight.exceptions.ConfigurationException
import org.dbpedia.spotlight.util.MathUtil


class SpotlightModel(val tokenizer: TextTokenizer,
                     val spotters: java.util.Map[SpotterPolicy, Spotter],
                     val disambiguators: java.util.Map[DisambiguationPolicy, ParagraphDisambiguatorJ],
                     val properties: Properties)

object SpotlightModel {
  //val featureNames = List("P(e|s)", "P(c|e)", "P(e)", "bias", "P(e|s)", "resource==sf")
  val featureNames = List("P(s|e)", "P(c|e)", "P(e)", "resource==sf", "bias")

  def loadStopwords(modelFolder: File): Set[String] = scala.io.Source.fromFile(new File(modelFolder, "stopwords.list")).getLines().map(_.trim()).toSet
  def loadSpotterThresholds(file: File): Seq[Double] = scala.io.Source.fromFile(file).getLines().next().split(" ").map(_.toDouble)

  def storesFromFolder(modelFolder: File): (TokenTypeStore, SurfaceFormStore, ResourceStore, CandidateMapStore, ContextStore, MemoryVectorStore) = {
    val modelDataFolder = new File(modelFolder, "model")

    List(
      new File(modelDataFolder, "tokens.mem"),
      new File(modelDataFolder, "sf.mem"),
      new File(modelDataFolder, "res.mem"),
      new File(modelDataFolder, "candmap.mem")
    ).foreach {
      modelFile: File =>
        if (!modelFile.exists())
          throw new IOException("Invalid Spotlight model folder: Could not read required file %s in %s.".format(modelFile.getName, modelFile.getPath))
    }

    val quantizedCountsStore = MemoryStore.loadQuantizedCountStore(new FileInputStream(new File(modelDataFolder, "quantized_counts.mem")))

    val tokenTypeStore = MemoryStore.loadTokenTypeStore(new FileInputStream(new File(modelDataFolder, "tokens.mem")))
    val sfStore = MemoryStore.loadSurfaceFormStore(new FileInputStream(new File(modelDataFolder, "sf.mem")), quantizedCountsStore)
    val resStore = MemoryStore.loadResourceStore(new FileInputStream(new File(modelDataFolder, "res.mem")), quantizedCountsStore)
    val candMapStore = MemoryStore.loadCandidateMapStore(new FileInputStream(new File(modelDataFolder, "candmap.mem")), resStore, quantizedCountsStore)


    val contextStore = if (new File(modelDataFolder, "vectors.mem").exists()){
      null
    } else if (new File(modelDataFolder, "context.mem").exists()) {
      MemoryStore.loadContextStore(new FileInputStream(new File(modelDataFolder, "context.mem")), tokenTypeStore, quantizedCountsStore)
    } else{
      null
    }

    val vectorStore = if (new File(modelDataFolder, "vectors.mem").exists()){
      MemoryStore.loadVectorStore(new FileInputStream(new File(modelDataFolder, "vectors.mem")))
    }else{
      null
    }



    (tokenTypeStore, sfStore, resStore, candMapStore, contextStore, vectorStore)
  }

  def fromFolder(modelFolder: File): SpotlightModel = {

    val (tokenTypeStore, sfStore, resStore, candMapStore, contextStore, vectorStore) = storesFromFolder(modelFolder)

    val stopwords = loadStopwords(modelFolder)

    val properties = new Properties()
    properties.load(new FileInputStream(new File(modelFolder, "model.properties")))

    //Read the version of the model folder. The lowest version supported by this code base is:
    val supportedVersion = 1.0
    val modelVersion = properties.getProperty("version", "0.1").toFloat
    if (modelVersion < supportedVersion)
      throw new ConfigurationException("Incompatible model version %s. This version of DBpedia Spotlight requires models of version 1.0 or newer. Please download a current model from http://spotlight.sztaki.hu/downloads/.".format(modelVersion))


    //Load the stemmer from the model file:
    def stemmer(): Stemmer = properties.getProperty("stemmer") match {
      case s: String if s equals "None" => null
      case s: String => new SnowballStemmer(s)
    }

    def contextSimilarity(): ContextSimilarity = {
      if (vectorStore != null) {
        new VectorContextSimilarity(tokenTypeStore, vectorStore)
      }else if (contextStore != null) {
        new GenerativeContextSimilarity(tokenTypeStore, contextStore)
      } else {
        new NoContextSimilarity(MathUtil.ln(1.0))
      }
    }

    val c = properties.getProperty("opennlp_parallel", Runtime.getRuntime.availableProcessors().toString).toInt
    val cores = (1 to c)

    val tokenizer: TextTokenizer = if(new File(modelFolder, "opennlp").exists()) {

      //Create the tokenizer:
      val posTagger = new File(modelFolder, "opennlp/pos-maxent.bin")
      val tokenizerModel = new TokenizerModel(new FileInputStream(new File(modelFolder, "opennlp/token.bin")))
      val sentenceModel = new SentenceModel(new FileInputStream(new File(modelFolder, "opennlp/sent.bin")))

      def createTokenizer() = new OpenNLPTokenizer(
        new TokenizerME(tokenizerModel),
        stopwords,
        stemmer(),
        new SentenceDetectorME(sentenceModel),
        if (posTagger.exists()) new POSTaggerME(new POSModel(new FileInputStream(posTagger))) else null,
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

    val mixture = if(new File(modelFolder, "ranklib-model.txt").exists()) {
      val weightsLineElements = scala.io.Source.fromFile(new File(modelFolder, "ranklib-model.txt")).getLines().toArray.last.split(" ")
      val weights = weightsLineElements.map { (elem: String) =>
        elem.substring(2).toDouble
      }
      if (weights.length == featureNames.length) {
        val featuresAndWeights: List[(String, Double)] = featureNames.zip(weights)

        SpotlightLog.info(this.getClass, "Using LLM weights: %s".format(
          featuresAndWeights.map {
            case (name, weight) =>
              "%s=%s".format(name, weight)
          }.reduce(_ + ", " + _)
        ))

        new LogLinearFeatureMixture(
          featuresAndWeights
        )
      }else{
        SpotlightLog.warn(this.getClass, "Could not load LLM weights, wrong number of weights in file! Using UnweightedMixture.")
        new UnweightedMixture(featureNames.toSet)
      }
    } else{
      SpotlightLog.info(this.getClass, "No weights found - using unweighted mixture.")
      new UnweightedMixture(featureNames.toSet)
    }

    val searcher      = new DBCandidateSearcher(resStore, sfStore, candMapStore)
    val disambiguator = new ParagraphDisambiguatorJ(new DBTwoStepDisambiguator(
      tokenTypeStore,
      sfStore,
      resStore,
      searcher,
      mixture,
      contextSimilarity()
    ))

    //If there is at least one NE model or a chunker, use the OpenNLP spotter:
    val spotter = if( new File(modelFolder, "opennlp").exists() && new File(modelFolder, "opennlp").list().exists(f => f.startsWith("ner-") || f.startsWith("chunker")) ) {
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
