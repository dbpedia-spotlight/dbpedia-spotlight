package org.dbpedia.spotlight.db.entitytopic

import java.io.{FileOutputStream, FileInputStream, File}
import org.dbpedia.spotlight.db.memory._
import java.util.{Locale, Properties}
import org.dbpedia.spotlight.db.model.{ResourceStore, SurfaceFormStore, TextTokenizer}
import org.dbpedia.spotlight.entitytopic.{AnnotatingMarkupParser, WikipediaRecordReader, Annotation}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.{SurfaceFormOccurrence, DBpediaResource, Text, SurfaceForm}
import opennlp.tools.util.Span
import org.dbpedia.spotlight.db.{SpotlightModel, DBCandidateSearcher, WikipediaToDBpediaClosure}
import scala.collection.mutable.ListBuffer
import org.apache.commons.logging.LogFactory
import java.util.concurrent.{ExecutorService, TimeUnit, Executors}


class EntityTopicModelTrainer( val wikiToDBpediaClosure:WikipediaToDBpediaClosure,
                        val tokenizer: TextTokenizer,
                        val searcher: DBCandidateSearcher,
                        val candMap: MemoryCandidateMapStore,
                        val properties: Properties
                        )  {
  val LOG = LogFactory.getLog(this.getClass)
  val sfStore: SurfaceFormStore = searcher.sfStore
  val resStore: ResourceStore = searcher.resStore

  val documents:ListBuffer[Document]=new ListBuffer[Document]()

  val localeCode = properties.getProperty("locale").split("_")
  val locale=new Locale(localeCode(0), localeCode(1))

  def learnFromWiki(wikidump:String, model_folder:String, threadNum:Int){
    LOG.info("Init wiki docs...")
    val start1 = System.currentTimeMillis()
    val counter=initializeWikiDocuments(wikidump,threadNum)
    LOG.info("Done (%d ms)".format(System.currentTimeMillis() - start1))

    Document.init(counter._1,counter._2,counter._3,candMap,properties)

    LOG.info("Update assignments...")
    val start2=System.currentTimeMillis()
    updateAssignments(1)//hardcode iterations of updates
    LOG.info("Done (%d ms)".format(System.currentTimeMillis() - start2))

    //save global knowledge/counters
    Document.entitymentionCount.writeToFile(model_folder+"/entitymention_count")
    Document.entitywordCount.writeToFile(model_folder+"/entityword_count")
    Document.topicentityCount.writeToFile(model_folder+"/topicentity_count")

    LOG.info("Finish training")
  }


  def updateAssignments(iterations:Int){
    val toal:Int=documents.size
    for(i <- 1 to iterations){
      var j:Int=0
      documents.foreach((doc:Document)=>{
        doc.updateAssignment(true)
        j+=1
        if(j%100==0)
          LOG.info("%d %% of %d-th iteration".format(j*100/toal, i))
      })
    }
  }


  /**
   *init the assignments for each document
   *
   * @param wikidump filename of the wikidump
   */
  def initializeWikiDocuments(wikidump:String, threadNum:Int):Triple[GlobalCounter,GlobalCounter,GlobalCounter]={
    val initializers=(0 until threadNum+1).map(_=>DocumentInitializer(tokenizer,searcher,properties,true))
    val pool=Executors.newFixedThreadPool(threadNum)

    var parsedDocs=0
    //parse wiki dump to get wiki pages iteratively
    val wikireader: WikipediaRecordReader = new WikipediaRecordReader(new File(wikidump))
    val converter: AnnotatingMarkupParser = new AnnotatingMarkupParser(locale.getLanguage())
    while(wikireader.nextKeyValue()){
      //val title:String=wikireader.getCurrentKey
      //val id:String=wikireader.getWikipediaId

      val content: String = converter.parse(wikireader.getCurrentValue)
      val annotations: List[Annotation]=converter.getWikiLinkAnnotations().toList

      //parse the wiki page to get link anchors: each link anchor has a surface form, dbpedia resource, span attribute
      val surfaceOccrs=new ListBuffer[SurfaceFormOccurrence]()
      val resources=new ListBuffer[DBpediaResource]()

      annotations.foreach((a: Annotation)=>{
        try{
          surfaceOccrs+=new SurfaceFormOccurrence(sfStore.getSurfaceForm(content.substring(a.begin,a.end)),null, a.begin)
          resources+=resStore.getResourceByName(wikiToDBpediaClosure.wikipediaToDBpediaURI(a.value))
        }catch{
          case _=>None
        }
      })

      if(resources.size>0){
        var idleInitializer=None.asInstanceOf[Option[DocumentInitializer]]
        while(idleInitializer==None)
          idleInitializer=initializers.find((initializer:DocumentInitializer)=>initializer.isRunning==false)
        val runner=idleInitializer.get
        runner.set(new Text(content),resources.toArray,surfaceOccrs.toArray)
        try{
          pool.execute(runner)
        }catch{
          case e: java.util.concurrent.TimeoutException=>{
            LOG.error("timeout exception")
          }
        }
        parsedDocs+=1
        if(parsedDocs%100==0)
          LOG.info("%d docs parsed".format(parsedDocs))
      }
    }
    shutdownAndAwaitTermination(pool)
    merge(initializers.toArray)
  }

  def merge(initializers:Array[DocumentInitializer]):Triple[GlobalCounter,GlobalCounter,GlobalCounter]={
    val ret=initializers(0)
    documents++=ret.documents
    (1 until initializers.length).foreach((i:Int)=>{
      ret.topicentityCount.add(initializers(i).topicentityCount)
      ret.entitymentionCount.add(initializers(i).entitymentionCount)
      ret.entitywordCount.add(initializers(i).entitywordCount)
      documents++=initializers(i).documents
    })

    Triple(ret.topicentityCount,ret.entitymentionCount,ret.entitywordCount)
  }

  def shutdownAndAwaitTermination(pool:ExecutorService) {
    pool.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(60, TimeUnit.SECONDS))
          LOG.info("Pool did not terminate");
      }
    } catch {
      case e:InterruptedException=>{
        // (Re-)Cancel if current thread also interrupted
        pool.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
    }
  }
}


object EntityTopicModelTrainer{

  def fromFolder(modelFolder: File, topicNum:Int): EntityTopicModelTrainer = {

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
      new FileInputStream(new File(modelFolder, "entitytopic/redirects.nt")),
      new FileInputStream(new File(modelFolder, "entitytopic/disambiguations.nt"))
    )

    val (tokenTypeStore, sfStore, resStore, candMapStore, _) = SpotlightModel.storesFromFolder(modelFolder)

    properties.setProperty("resourceNum", resStore.asInstanceOf[MemoryResourceStore].size.toString)
    properties.setProperty("surfaceNum", sfStore.asInstanceOf[MemorySurfaceFormStore].size.toString)
    properties.setProperty("tokenNum", tokenTypeStore.asInstanceOf[MemoryTokenTypeStore].size.toString)
    properties.setProperty("topicNum", topicNum.toString)
    properties.setProperty("alpha",(50/topicNum.toFloat).toString)
    properties.setProperty("beta", "0.1")
    properties.setProperty("gama", (1.0/sfStore.asInstanceOf[MemorySurfaceFormStore].size).toString)
    properties.setProperty("delta", (2000.0/tokenTypeStore.asInstanceOf[MemoryTokenTypeStore].size).toString)
    properties.store(new FileOutputStream(new File(modelFolder,"model.properties")), "add properties for entity topic model")

    val tokenizer: TextTokenizer= SpotlightModel.createTokenizer(modelFolder,tokenTypeStore,properties,stopwords,cores)
    val searcher:DBCandidateSearcher = new DBCandidateSearcher(resStore, sfStore, candMapStore)

    new EntityTopicModelTrainer(wikipediaToDBpediaClosure,tokenizer, searcher, candMapStore.asInstanceOf[MemoryCandidateMapStore], properties)
  }


  def main(args:Array[String]){
    val model_dir=args(0)
    val wikidump=args(1)

    System.out.println(model_dir, wikidump)

    var threadNum=5
    var topicNum=100
    if(args.length>2)
      threadNum=args(2).toInt
    if(args.length>3)
      topicNum=args(3).toInt

    val trainer:EntityTopicModelTrainer=EntityTopicModelTrainer.fromFolder(new File(model_dir),topicNum)
    trainer.learnFromWiki(wikidump, model_dir+"/entitytopic", threadNum)
  }
}
