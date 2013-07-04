package org.dbpedia.spotlight.db.entityTopic

import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.db.memory.{DocumentStore, MemoryStore}
import java.util.{Locale, Properties}
import org.dbpedia.spotlight.db.model.{TextTokenizer, Stemmer}
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import org.dbpedia.spotlight.db.tokenize.{LanguageIndependentTokenizer, OpenNLPTokenizer}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import org.dbpedia.spotlight.db.concurrent.{SpotterWrapper, TokenizerWrapper}
import org.dbpedia.spotlight.db._
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.entityTopic.{AnnotatingMarkupParser, WikipediaRecordReader, Annotation}
import java.lang.String
import java.util
import org.dbpedia.spotlight.model.{Text, SurfaceForm, DBpediaResource}
import opennlp.tools.util.Span


class CreateEntityTopicModel( val locale:Locale,
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

object CreateEntityTopicModel{

  def fromFolder(modelFolder: File): CreateEntityTopicModel = {

    val properties = new Properties()
    properties.load(new FileInputStream(new File(modelFolder, "model.properties")))

    val stopwords = SpotlightModel.loadStopwords(modelFolder)
    val c = properties.getProperty("opennlp_parallel", Runtime.getRuntime.availableProcessors().toString).toInt
    val cores = (1 to c)

    val localeCode = properties.getProperty("locale").split("_")
    val locale=new Locale(localeCode(0), localeCode(1))
    val namespace = if (locale.getLanguage.equals("en")) {
      "http://dbpedia.org/resource/"
    } else {
      "http://%s.dbpedia.org/resource/".format(locale.getLanguage)
    }

    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      namespace,
      new FileInputStream(new File(modelFolder, "redirects.nt")),
      new FileInputStream(new File(modelFolder, "disambiguations.nt"))
    )

    val (tokenTypeStore, sfStore, resStore, candMapStore, _) = SpotlightModel.storesFromFolder(modelFolder)

    val tokenizer: TextTokenizer= SpotlightModel.createTokenizer(modelFolder,tokenTypeStore,properties,stopwords,cores)
    val spotter:Spotter= SpotlightModel.createSpotter(modelFolder,sfStore,stopwords,cores)
    val searcher:DBCandidateSearcher = new DBCandidateSearcher(resStore, sfStore, candMapStore)

    new CreateEntityTopicModel(locale,wikipediaToDBpediaClosure,tokenizer, spotter, searcher)
  }

  def main(args:Array[String]){
    val model:CreateEntityTopicModel=CreateEntityTopicModel.fromFolder(new File("C:\\Users\\a0082293\\Documents\\GitHub\\data\\spotlight\\nl\\model_nl"))

    model.learnFromWiki("../data/test.xml")

  }
}
