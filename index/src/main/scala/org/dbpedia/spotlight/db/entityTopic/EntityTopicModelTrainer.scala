package org.dbpedia.spotlight.db.entitytopic

import java.io.{FileOutputStream, FileInputStream, File}
import org.dbpedia.spotlight.db.memory._
import java.util.{Locale, Properties}
import org.dbpedia.spotlight.db.model.{ResourceStore, SurfaceFormStore, TextTokenizer}
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.entitytopic.{AnnotatingMarkupParser, WikipediaRecordReader, Annotation}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.{Text}
import opennlp.tools.util.Span
import org.dbpedia.spotlight.db.{SpotlightModel, DBCandidateSearcher, WikipediaToDBpediaClosure}
import scala.collection.mutable.ListBuffer


class EntityTopicModelTrainer( val wikiToDBpediaClosure:WikipediaToDBpediaClosure,
                        val tokenizer: TextTokenizer,
                        val searcher: DBCandidateSearcher,
                        val candMap: MemoryCandidateMapStore,
                        val properties: Properties
                        )  {
  val docStore: DocumentStore = new DocumentStore()
  val sfStore: SurfaceFormStore = searcher.sfStore
  val resStore: ResourceStore = searcher.resStore

  val documents:ListBuffer[Document]=new ListBuffer[Document]()

  val localeCode = properties.getProperty("locale").split("_")
  val locale=new Locale(localeCode(0), localeCode(1))

  def learnFromWiki(wikidump:String){
    initializeWikiDocuments(wikidump)
    updateAssignments(1)//hardcode iterations of updates
  }


  def updateAssignments(iterations:Int){
    for(i <- 1 to iterations){
      documents.foreach((doc:Document)=>doc.updateAssignment())
    }
  }


  /**
   *init the assignments for each document
   *
   * @param wikidump filename of the wikidump
   */
  def initializeWikiDocuments(wikidump:String){
    Document.init(candMap, properties)
    DocumentInitializer.init(this,properties.getProperty("topicNum").toInt, properties.getProperty("maxSurfaceformLen").toInt)//hardcode the topic number

    //parse wiki dump to get wiki pages iteratively
    val wikireader: WikipediaRecordReader = new WikipediaRecordReader(new File(wikidump))
    val converter: AnnotatingMarkupParser = new AnnotatingMarkupParser(locale.getLanguage())

    while(wikireader.nextKeyValue()){
      //val title:String=wikireader.getCurrentKey
      //val id:String=wikireader.getWikipediaId

      val content: String = converter.parse(wikireader.getCurrentValue)
      val annotations: List[Annotation]=converter.getWikiLinkAnnotations().toList

      //parse the wiki page to get link anchors: each link anchor has a surface form, dbpedia resource, span attribute
      val surfaces = annotations.map((a: Annotation)=> sfStore.getSurfaceForm(content.substring(a.begin,a.end))).toArray
      val resources = annotations.map((a: Annotation) => resStore.getResourceByName(wikiToDBpediaClosure.wikipediaToDBpediaURI(a.value))).toArray
      val spans = annotations.map((a: Annotation) => new Span(a.begin,a.end)).toArray

      documents+=DocumentInitializer.initDocument(new Text(content),resources,surfaces,spans)
    }
  }
}


object EntityTopicModelTrainer{

  def fromFolder(modelFolder: File): EntityTopicModelTrainer = {

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

    properties.setProperty("resourceNum", resStore.asInstanceOf[MemoryResourceStore].size.toString)
    properties.setProperty("surfaceNum", sfStore.asInstanceOf[MemorySurfaceFormStore].size.toString)
    properties.setProperty("tokenNum", tokenTypeStore.asInstanceOf[MemoryTokenTypeStore].size.toString)
    properties.setProperty("alpha",(50/properties.getProperty("topicNum").toFloat).toString)
    properties.setProperty("beta", "0.1")
    properties.setProperty("gama", (1.0/sfStore.asInstanceOf[MemorySurfaceFormStore].size).toString)
    properties.setProperty("delta", (2000.0/tokenTypeStore.asInstanceOf[MemoryTokenTypeStore].size).toString)
    properties.store(new FileOutputStream(new File(modelFolder,"model.properties")), "add properties for entity topic model")

    val tokenizer: TextTokenizer= SpotlightModel.createTokenizer(modelFolder,tokenTypeStore,properties,stopwords,cores)
    val searcher:DBCandidateSearcher = new DBCandidateSearcher(resStore, sfStore, candMapStore)

    new EntityTopicModelTrainer(wikipediaToDBpediaClosure,tokenizer, searcher, candMapStore.asInstanceOf[MemoryCandidateMapStore], properties)
  }


  def main(args:Array[String]){
    val model:EntityTopicModelTrainer=EntityTopicModelTrainer.fromFolder(new File("C:\\Users\\a0082293\\Documents\\GitHub\\data\\spotlight\\nl\\model_nl"))
    model.learnFromWiki("../data/test.xml")
  }
}
