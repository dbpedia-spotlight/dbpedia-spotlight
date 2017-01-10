package org.dbpedia.spotlight.db

import java.util

import concurrent.{TokenizerWrapper, SpotterWrapper}
import org.dbpedia.spotlight.db.memory.{MemoryContextStore, MemoryStore}
import model._
import opennlp.tools.tokenize.{TokenizerModel, TokenizerME}
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetectorME}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import org.dbpedia.spotlight.disambiguate.mixtures.UnweightedMixture
import org.dbpedia.spotlight.db.similarity.{NoContextSimilarity, GenerativeContextSimilarity, ContextSimilarity}
import org.dbpedia.spotlight.filter.annotations.FilterPolicy$
import org.dbpedia.spotlight.filter.visitor.{FilterOccsImpl, OccsFilter, FilterElement}
import org.dbpedia.spotlight.model.Factory.Paragraph
import org.dbpedia.spotlight.model._
import scala.collection.JavaConverters._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ
import org.dbpedia.spotlight.spot.{SpotXmlParser, Spotter}
import java.io.{IOException, File, FileInputStream}
import java.util.{Locale, Properties}
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.namefind.TokenNameFinderModel
import stem.SnowballStemmer
import tokenize.{OpenNLPTokenizer, LanguageIndependentTokenizer}
import org.dbpedia.spotlight.exceptions.{SearchException, InputException, ConfigurationException}
import org.dbpedia.spotlight.util.MathUtil
import scala.collection.JavaConversions._

class BaseSpotlightModel(val tokenizer: TextTokenizer,
                     val spotters: java.util.Map[SpotterPolicy, Spotter],
                     val disambiguators: java.util.Map[DisambiguationPolicy, ParagraphDisambiguatorJ],
                     val properties: Properties) extends SpotlightModel{


  def nBest(stringText: String, params: AnnotationParameters,n: Int):  java.util.Map[SurfaceFormOccurrence, java.util.List[DBpediaResourceOccurrence]] = {

    val filteredEntityCandidates: scala.collection.mutable.Map[SurfaceFormOccurrence, java.util.List[DBpediaResourceOccurrence]] = new scala.collection.mutable.HashMap[SurfaceFormOccurrence, java.util.List[DBpediaResourceOccurrence]]()

    val spotter = this.getSpotter(params.spotterName)
    val disambiguator = this.getDisambiguator(params.disambiguatorName)

    val textObject = new Text(stringText)
    textObject.setFeature(new Score("confidence", params.disambiguationConfidence))
    textObject.setFeature(new Score("spotterConfidence", params.spotterConfidence))

    if(tokenizer != null)
      tokenizer.tokenizeMaybe(textObject)

    val entityMentions = spotter.extract(textObject)
    if(entityMentions.size() == 0) return filteredEntityCandidates;
    val paragraph = Factory.paragraph().fromJ(entityMentions);

    val entityCandidates: java.util.Map[SurfaceFormOccurrence, java.util.List[DBpediaResourceOccurrence]] = disambiguator.bestK(paragraph, n)


    /*The previous addition of filter to the Candidates requests (which has usability questioned) produce the error described at issue #136.
              To solve it, this feature for this argument (Candidates) is disabled, setting coreferenceResolution to false ever. Ignoring the user's configuration.
            */
    val unableCoreferenceResolution = false


    val filter: FilterElement = new OccsFilter(params.spotterConfidence,
                                               params.support,
                                               params.dbpediaTypes,
                                               params.sparqlQuery,
                                               params.blacklist,
                                               unableCoreferenceResolution,
                                               params.similarityThresholds,
                                               params.sparqlExecuter)

    entityCandidates.entrySet().foreach{
       entry: java.util.Map.Entry[SurfaceFormOccurrence, java.util.List[DBpediaResourceOccurrence]] =>
         val result: java.util.List[DBpediaResourceOccurrence] = filter.accept(new FilterOccsImpl, entry.getValue)
         if(!result.isEmpty()) filteredEntityCandidates.put(entry.getKey, result)
    }

    filteredEntityCandidates

  }

  def disambiguate(spots: java.util.List[SurfaceFormOccurrence], disambiguator: ParagraphDisambiguatorJ): java.util.List[DBpediaResourceOccurrence] = {
    var resources: java.util.List[DBpediaResourceOccurrence] = new util.ArrayList[DBpediaResourceOccurrence]()

    if (spots.size == 0) return resources

    if (tokenizer != null)
        tokenizer.tokenizeMaybe(spots.get(0).context)

    try {
      resources = disambiguator.disambiguate(Factory.paragraph.fromJ(spots))
    }
    catch {
      case e: UnsupportedOperationException => {
        throw new SearchException(e)
      }
    }
    return resources
  }


 def spot(context: Text, params: AnnotationParameters): java.util.List[SurfaceFormOccurrence]={
    val spotter = getSpotter(params.spotterName)

    if (this.tokenizer != null)
      tokenizer.tokenizeMaybe(context)

    val spots: java.util.List[SurfaceFormOccurrence] = spotter.extract(context)

    spots
  }

  def firstBest(text: String, params: AnnotationParameters): java.util.List[DBpediaResourceOccurrence]={

    // Get input text
    if (text.trim == "") {
      throw new InputException("No text was specified in the &text parameter.")
    }

    val context: Text = new Text(text)

    context.setFeature(new Score("spotterConfidence", params.spotterConfidence))
    context.setFeature(new Score("disambiguationConfidence", params.disambiguationConfidence))

    // Find spots to annotate/disambiguate
    val spots: java.util.List[SurfaceFormOccurrence] = spot(context, params)

    // Call annotation or disambiguation
    val maxLengthForOccurrenceCentric: Int = 1200

    if (tokenizer == null &&
       (params.disambiguatorName == SpotlightConfiguration.DisambiguationPolicy.Default.name) &&
       text.length > maxLengthForOccurrenceCentric) {
              params.disambiguatorName = SpotlightConfiguration.DisambiguationPolicy.Document.name
//             LOG.info(String.format("Text length > %d. Using %s to disambiguate.", maxLengthForOccurrenceCentric, params.disambiguatorName))
    }


    val disambiguator: ParagraphDisambiguatorJ = params.disambiguator
    var occList: java.util.List[DBpediaResourceOccurrence] = disambiguate(spots, disambiguator)
    val filter: FilterElement = new OccsFilter(params.disambiguationConfidence,
                                               params.support,
                                               params.dbpediaTypes,
                                               params.sparqlQuery,
                                               params.blacklist,
                                               params.coreferenceResolution,
                                               params.similarityThresholds,
                                               params.sparqlExecuter)

    occList = filter.accept(new FilterOccsImpl, occList)


    //if (LOG.isDebugEnabled) {
     // LOG.debug("Shown:")

      //occList.foreach{
      //    occ:DBpediaResourceOccurrence =>
          // LOG.debug(String.format("%s <- %s; score: %s, ctxscore: %3.2f, support: %s, prior: %s", occ.resource, occ.surfaceForm, occ.similarityScore, occ.contextualScore, occ.resource.support, occ.resource.prior))
      //}

   // }



    return occList
  }


}

object BaseSpotlightModel {

  def loadStopwords(modelFolder: File): Set[String] = scala.io.Source.fromFile(new File(modelFolder, "stopwords.list")).getLines().map(_.trim()).toSet
  def loadSpotterThresholds(file: File): Seq[Double] = scala.io.Source.fromFile(file).getLines().next().split(" ").map(_.toDouble)

  def storesFromFolder(modelFolder: File): (TokenTypeStore, SurfaceFormStore, ResourceStore, CandidateMapStore, ContextStore) = {
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
    val contextStore = if (new File(modelDataFolder, "context.mem").exists())
      MemoryStore.loadContextStore(new FileInputStream(new File(modelDataFolder, "context.mem")), tokenTypeStore, quantizedCountsStore)
    else
      null

    (tokenTypeStore, sfStore, resStore, candMapStore, contextStore)
  }

  def fromFolder(modelFolder: File): BaseSpotlightModel = {

    val (tokenTypeStore, sfStore, resStore, candMapStore, contextStore) = storesFromFolder(modelFolder)

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

    def contextSimilarity(): ContextSimilarity = contextStore match {
      case store:MemoryContextStore => new GenerativeContextSimilarity(tokenTypeStore, contextStore)
      case _ => new NoContextSimilarity(MathUtil.ln(1.0))
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

    val searcher      = new DBCandidateSearcher(resStore, sfStore, candMapStore)
    val disambiguator = new ParagraphDisambiguatorJ(new DBTwoStepDisambiguator(
      tokenTypeStore,
      sfStore,
      resStore,
      searcher,
      new UnweightedMixture(Set("P(e)", "P(c|e)", "P(s|e)")),
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
    new BaseSpotlightModel(tokenizer, spotters, disambiguators, properties)
  }
}
