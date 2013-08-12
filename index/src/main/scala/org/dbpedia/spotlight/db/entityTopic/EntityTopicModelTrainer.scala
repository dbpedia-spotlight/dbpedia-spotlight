package org.dbpedia.spotlight.db.entitytopic

import java.io.{FileOutputStream, FileInputStream, File}
import org.dbpedia.spotlight.db.memory._
import java.util.{Locale, Properties}
import org.dbpedia.spotlight.db.model.{ResourceStore, SurfaceFormStore, TextTokenizer}
import org.dbpedia.spotlight.entitytopic.{AnnotatingMarkupParser, WikipediaRecordReader, Annotation}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.db.{SpotlightModel, DBCandidateSearcher, WikipediaToDBpediaClosure}
import scala.collection.mutable.ListBuffer
import org.apache.commons.logging.LogFactory
import java.util.concurrent.{ExecutorService, TimeUnit, Executors}
import org.dbpedia.spotlight.model.Factory.DBpediaResourceOccurrence
import org.dbpedia.spotlight.exceptions.{NotADBpediaResourceException, SurfaceFormNotFoundException, DBpediaResourceNotFoundException}
import org.dbpedia.spotlight.db.entitytopic.DocumentCorpus


class EntityTopicModelTrainer( val wikiToDBpediaClosure:WikipediaToDBpediaClosure,
                        val tokenizer: TextTokenizer,
                        val searcher: DBCandidateSearcher,
                        val candMap: MemoryCandidateMapStore,
                        val properties: Properties
                        )  {
  val LOG = LogFactory.getLog(this.getClass)
  val sfStore: SurfaceFormStore = searcher.sfStore
  val resStore: ResourceStore = searcher.resStore
  //val gibbsSteps=properties.getProperty("gibbsSteps").toInt

  val docCorpusList=new ListBuffer[DocumentCorpus]()

  val localeCode = properties.getProperty("locale").split("_")
  val locale=new Locale(localeCode(0), localeCode(1))

  def createDir(dir:String){
    val dirFile = new File(dir)
    if(!dirFile.exists())
      dirFile.mkdir()
  }

  def readGlobalCounters(dir:String):Triple[GlobalCounter,GlobalCounter,GlobalCounter]={
    val entitymention=GlobalCounter.readFromFile(dir+"/entitymention_count")
    val entityword=GlobalCounter.readFromFile(dir+"/entityword_count")
    val topicentity=GlobalCounter.readFromFile(dir+"/topicentity_count")
    new Triple(entitymention,entityword,topicentity)
  }

  def saveGlobalCounters(dir:String){
    createDir(dir)
    Document.entitymentionCount.writeToFile(dir+"/entitymention_count")
    Document.entitywordCount.writeToFile(dir+"/entityword_count")
    Document.topicentityCount.writeToFile(dir+"/topicentity_count")
  }

  def learnFromWiki(wikidump:String, model_folder:String, threadNum:Int, gibbsSteps:Int, parseWiki:Boolean){
    if(parseWiki){
      createDir(model_folder)

      LOG.info("Init wiki docs...")
      val start = System.currentTimeMillis()
      val globalcounters=initializeWikiDocuments(wikidump,model_folder, threadNum)
      LOG.info("Done (%d ms)".format(System.currentTimeMillis() - start))

      Document.init(globalcounters._1,globalcounters._2,globalcounters._3,candMap,properties)
      saveGlobalCounters(model_folder+"/tmpcounters")
     }else{
      LOG.info("load documents...")
      val tmpcorpus=new File(model_folder+"/tmpcorpus")
      val corpus=tmpcorpus.listFiles()
      corpus.foreach((c:File)=>{
        docCorpusList+=new DocumentCorpus(c.getName)
      })

      LOG.info("load global counters...")
      val globalcounters=readGlobalCounters(model_folder+"/tmpcounters")
      Document.init(globalcounters._1,globalcounters._2,globalcounters._3,candMap,properties)
    }

    LOG.info("Update assignments...")
    val start=System.currentTimeMillis()
    updateAssignments(gibbsSteps)
    LOG.info("Done (%d ms)".format(System.currentTimeMillis() - start))

    //save global knowledge/counters
    saveGlobalCounters(model_folder)
    LOG.info("Finish training")
  }


  def updateAssignments(iterations:Int){
    val total=docCorpusList.foldLeft[Int](0)((num:Int, corpus:DocumentCorpus)=>num+corpus.total)
    for(i <- 1 to iterations){
      var j:Int=0
      docCorpusList.foreach((corpus:DocumentCorpus)=>{
        val documents=corpus.loadDocs()
        documents.foreach((doc:Document)=>{
          doc.updateAssignment(true)
          j+=1
          if(j%100==0)
            LOG.info("%d %% of %d-th iteration".format(j*100/total, i))
        })
      })
    }
  }


  /**
   *init the assignments for each document
   * multiple DocmentInitializers works parallelly
   *
   * @param wikidump filename of the wikidump
   */
  def initializeWikiDocuments(wikidump:String, model_folder: String, threadNum:Int):Triple[GlobalCounter,GlobalCounter,GlobalCounter]={
    createDir(model_folder+"/tmpcorpus")
    val initializers=(0 until threadNum+1).map((k:Int)=>{
      val docTmpStore=model_folder+"/tmpcorpus/"+k.toString()
      DocumentInitializer(tokenizer,searcher,properties,docTmpStore, true)
    })
    val pool=Executors.newFixedThreadPool(threadNum)

    //parse wiki dump
    val wikireader: WikipediaRecordReader = new WikipediaRecordReader(new File(wikidump))
    val converter: AnnotatingMarkupParser = new AnnotatingMarkupParser(locale.getLanguage())

    var parsedDocs=0
    var parsedRes=0
    var unknownSF=0
    var unknownRes=0
    var emptyCands=0
    while(wikireader.nextKeyValue()){
      val content = converter.parse(wikireader.getCurrentValue)
      val annotations=converter.getWikiLinkAnnotations().toList

      //parse the wiki page to get link anchors: each link anchor is wrapped into a dbpediaresourceoccrrence instance
      val resOccrs=new ListBuffer[DBpediaResourceOccurrence]()
      annotations.foreach((a: Annotation)=>{
        try{
          val sfOccr=new SurfaceFormOccurrence(sfStore.getSurfaceForm(content.substring(a.begin,a.end)),new Text(content), a.begin)
          if(candMap.getCandidates(sfOccr.surfaceForm).size>0){
            val res=resStore.getResourceByName(wikiToDBpediaClosure.wikipediaToDBpediaURI(a.value))
            resOccrs+=DBpediaResourceOccurrence.from(sfOccr, res, 0.0)
            parsedRes+=1
          }else{
            emptyCands+=1
          }
        }catch{
          case e:DBpediaResourceNotFoundException=>{unknownRes+=1}
          case e:SurfaceFormNotFoundException=>{unknownSF+=1}
          case e:NotADBpediaResourceException=>{unknownRes+=1}
        }
      })


      if(resOccrs.size>0){
        var idleInitializer=None.asInstanceOf[Option[DocumentInitializer]]
        while(idleInitializer==None)
          idleInitializer=initializers.find((initializer:DocumentInitializer)=>initializer.isRunning==false)
        val runner=idleInitializer.get
        runner.set(new Text(content),resOccrs.toArray)
        try{
          pool.execute(runner)
        }catch{
          case e: java.util.concurrent.TimeoutException=>{
            LOG.error("timeout exception")
          }
        }
        parsedDocs+=1
        if(parsedDocs%100==0){
          val memLoaded = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
          LOG.info("%d docs parsed, parsed res %d unknown sf %d, unknown res %d, empty cands %d, mem %d M".format(
            parsedDocs, parsedRes, unknownSF, unknownRes, emptyCands, memLoaded))
        }
      }
    }
    shutdownAndAwaitTermination(pool)
    merge(initializers.toArray)
  }

  def merge(initializers:Array[DocumentInitializer]):Triple[GlobalCounter,GlobalCounter,GlobalCounter]={
    val ret=initializers(0)
    docCorpusList+=ret.docCorpus
    ret.docCorpus.closeOutputStream()
    (1 until initializers.length).foreach((i:Int)=>{
      ret.topicentityCount.add(initializers(i).topicentityCount)
      ret.entitymentionCount.add(initializers(i).entitymentionCount)
      ret.entitywordCount.add(initializers(i).entitywordCount)
      docCorpusList+=initializers(i).docCorpus
      initializers(i).docCorpus.closeOutputStream()
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
    var spotlightmodel_path="model"
    var data_path="data"
    var entitytopicmodel_path="entitytopic"
    var threadNum=2
    var gibbsSteps=10
    var topicNum=100
    var init=false

    var i=0
    while(i<args.length){
      if(args(i)=="-spotlight"){
        spotlightmodel_path=args(i+1)
        i+=2
      }else if(args(i)=="-data"){
        data_path=args(i+1)
        i+=2
      }else if(args(i)=="-entitytopic"){
        entitytopicmodel_path=args(i+1)
        i+=2
      }else if(args(i)=="-threads"){
        threadNum=args(i+1).toInt
        i+=2
      }else if(args(i)=="-gibbs"){
        gibbsSteps=args(i+1).toInt
        i+=2
      }else if(args(i)=="-topics"){
        topicNum=args(i+1).toInt
        i+=2
      }else if(args(i)=="-init"){
        init=true
        i+=1
      }else{
        i+1
      }
    }

    val trainer:EntityTopicModelTrainer=EntityTopicModelTrainer.fromFolder(new File(spotlightmodel_path),topicNum)
    trainer.learnFromWiki(data_path, entitytopicmodel_path, threadNum, gibbsSteps,init)
  }
}
