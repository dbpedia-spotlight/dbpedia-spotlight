package org.dbpedia.spotlight.db.entityTopic

import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.db.memory.{DocumentStore, MemoryStore}
import java.util.{Locale, Properties}
import org.dbpedia.spotlight.db.model.{TextTokenizer, Stemmer}
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import org.dbpedia.spotlight.db.tokenize.{LanguageIndependentTokenizer, OpenNLPTokenizer}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import org.dbpedia.spotlight.db.concurrent.{SpotterWrapper, TokenizerWrapper}
import org.dbpedia.spotlight.db.{WikipediaToDBpediaClosure, FSASpotter, OpenNLPSpotter, DBCandidateSearcher}

import opennlp.tools.namefind.TokenNameFinderModel
import opennlp.tools.chunker.ChunkerModel
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.entityTopic.{AnnotatingMarkupParser, WikipediaRecordReader, Annotation}
import java.lang.String

import java.util
import org.dbpedia.spotlight.model.{Text, SurfaceForm, DBpediaResource}
import opennlp.tools.util.Span
import org.dbpedia.spotlight.db.entityTopic.Document



class EntityTopicModel( val locale:Locale,
                        val wikiToDBpediaClosure:WikipediaToDBpediaClosure,
                        val tokenizer: TextTokenizer,
                        val spotter: Spotter,
                        val searcher: DBCandidateSearcher
                        )  {
  val docStore:DocumentStore=new DocumentStore()
  val sfStore=searcher.sfStore
  val resStore=searcher.resStore




  def learnFromWiki(wikidump:String){

    initializeDocuments(wikidump)

    //TODO: update the assignments and counts through gibbs sampling
  }


  /**
   *init the assignments for each document
   *
   * @param wikidump filename of the wikidump
   */
  def initializeDocuments(wikidump:String){
    docStore.initSave() //prepare for saving the document assignments to tmp file
    DocumentObj.init(this,100)//hardcode the topic number

    //parse wiki dump to get wiki pages iteratively
    val wikireader: WikipediaRecordReader = new WikipediaRecordReader(new File(wikidump))
    val converter: AnnotatingMarkupParser = new AnnotatingMarkupParser(locale.getLanguage())

    while(wikireader.nextKeyValue()){
      val title:String=wikireader.getCurrentKey
      val id:String=wikireader.getWikipediaId

      val content: String = converter.parse(wikireader.getCurrentValue)
      val annotations:util.List[Annotation]=converter.getWikiLinkAnnotations()

      val resources:Array[DBpediaResource]=new Array[DBpediaResource](annotations.size())
      val surfaces:Array[SurfaceForm]=new Array[SurfaceForm](annotations.size())
      val spans:Array[Span]=new Array[Span](annotations.size())

      //parse the wiki page to get link anchors: each link anchor has a surface form, dbpedia resource, span attribute
      var i:Int=0
      while(i<annotations.size()){
        val a:Annotation=annotations.get(i)
        surfaces(i)=sfStore.getSurfaceForm(content.substring(a.begin,a.end))
        resources(i)=resStore.getResourceByName(wikiToDBpediaClosure.wikipediaToDBpediaURI(a.value))//a.value.replaceFirst(namespace,"")
        spans(i)=new Span(a.begin,a.end)
        i+=1
      }

      val doc:Document=DocumentObj.initDocument(new Text(content),resources,surfaces,spans)
      docStore.save(doc)
    }

    docStore.finishSave()
  }

}

object EntityTopicModel{
  def loadStopwords(modelFolder: File): Set[String] = scala.io.Source.fromFile(new File(modelFolder, "stopwords.list")).getLines().map(_.trim()).toSet
  def loadSpotterThresholds(file: File): Seq[Double] = scala.io.Source.fromFile(file).getLines().next().split(" ").map(_.toDouble)

  def fromFolder(modelFolder: File): EntityTopicModel = {

    val modelDataFolder = new File(modelFolder, "model")
    val tokenTypeStore = MemoryStore.loadTokenTypeStore(new FileInputStream(new File(modelDataFolder, "tokens.mem")))
    val sfStore =        MemoryStore.loadSurfaceFormStore(new FileInputStream(new File(modelDataFolder, "sf.mem")))
    val resStore =       MemoryStore.loadResourceStore(new FileInputStream(new File(modelDataFolder, "res.mem")))
    val candMapStore =   MemoryStore.loadCandidateMapStore(new FileInputStream(new File(modelDataFolder, "candmap.mem")), resStore)

    val stopwords = loadStopwords(modelFolder)

    val properties = new Properties()
    properties.load(new FileInputStream(new File(modelFolder, "model.properties")))
    val localeCode = properties.getProperty("locale").split("_")
    val locale=new Locale(localeCode(0), localeCode(1))

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

      new LanguageIndependentTokenizer(stopwords, stemmer(), locale, tokenTypeStore)
    }

    val searcher = new DBCandidateSearcher(resStore, sfStore, candMapStore)

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


    val namespace = if (locale.getLanguage.equals("en")) {
      "http://wikipedia.org/wiki/"
    } else {
      "http://%s.wikipedia.org/wiki/".format(locale.getLanguage)
    }

    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      namespace,
      new FileInputStream(new File(modelFolder, "redirects.nt")),
      new FileInputStream(new File(modelFolder, "disambiguations.nt"))
    )

    new EntityTopicModel(locale,wikipediaToDBpediaClosure,tokenizer, spotter, searcher)
  }

  def main(args:Array[String]){
    val model:EntityTopicModel=EntityTopicModel.fromFolder(new File("C:\\Users\\a0082293\\Documents\\GitHub\\data\\spotlight\\nl\\model_nl"))

    model.learnFromWiki("../data/test.xml")

  }
}
