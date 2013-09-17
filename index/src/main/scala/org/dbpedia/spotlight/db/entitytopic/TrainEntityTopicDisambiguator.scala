package org.dbpedia.spotlight.db.entitytopic

import java.io.{FileOutputStream, FileInputStream, File}
import org.dbpedia.spotlight.db.memory._
import java.util.{Locale, Properties}
import org.dbpedia.spotlight.db.model.TextTokenizer
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.db._
import scala.collection.mutable.ListBuffer
import org.apache.commons.logging.LogFactory
import java.util.concurrent.{ExecutorService, TimeUnit, Executors}
import org.apache.commons.io.FileUtils
import org.dbpedia.extraction.wikiparser.{PageNode, WikiParser}
import org.dbpedia.extraction.sources.{WikiPage, XMLSource}
import org.dbpedia.extraction.util.Language
import org.dbpedia.spotlight.string.WikiMarkupStripper
import org.dbpedia.spotlight.io.{WikiOccurrenceSource, WikiPageUtil}
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.model.Paragraph
import org.dbpedia.spotlight.exceptions.{SpottingException, SurfaceFormNotFoundException, DBpediaResourceNotFoundException}
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator
import org.dbpedia.spotlight.disambiguate.mixtures.UnweightedMixture
import org.dbpedia.spotlight.db.similarity.GenerativeContextSimilarity


/**
 * Learn global knowledge from wikipeida dump accroding to entity topic model
 * We construct a Document instance for each wiki page by assigning every mention(surface form)/token in with an
 * entity(DBpediaResource), and assigning every entity with a topic. The learning process is to update each document's
 * entity-mention, entity-word, topic-entity assignments. The global knowledge is reflected by these three assignments.
 *
 * ref: Xianpei Han, Le Sun: An Entity-Topic Model for Entity Linking. EMNLP-CoNLL 2012: 105-115
 *
 * @param wikiToDBpediaClosure
 * @param tokenizer
 * @param searcher
 * @param candMap
 * @param disambiguator
 * @param spotter
 * @param properties
 */

class TrainEntityTopicDisambiguator( val wikiToDBpediaClosure:WikipediaToDBpediaClosure,
                        val tokenizer: TextTokenizer,
                        val searcher: DBCandidateSearcher,
                        val candMap: MemoryCandidateMapStore,
                        val disambiguator:ParagraphDisambiguator,
                        val spotter: Spotter,
                        val properties: Properties
                        )  {
  val LOG = LogFactory.getLog(this.getClass)

  //*CounterSum is to aggregate samples during gibbs sampling.
  var entitymentionCounterSum:GlobalCounter=null
  var entitywordCounterSum:GlobalCounter=null
  var topicentityCounterSum:GlobalCounter=null

  val wikiParser = WikiParser.getInstance("entitytopic")

  def createDir(dir:String){
    val dirFile = new File(dir)
    if(!dirFile.exists())
      dirFile.mkdir()
  }

  /**
   * do checkpoint during training, i.e.,
   * do a snapshot for doc corpus and counter files
   *
   * @param epoch
   * @param dir snapshot directory path
   */
  def doCheckpoint(epoch:Int,dir:String){
    copyDir(dir+"/traincorpus",dir+"/ck"+epoch)
  }


  def copyDir(from:String,to:String){
    val from_dir=new File(from)
    val to_dir=new File(to)
    if (to_dir.exists())
      FileUtils.deleteDirectory(to_dir)
    FileUtils.copyDirectory(from_dir,to_dir)
  }

  /**
   * load global counters into memory
   * if aggregate counter(ending with _sum) files exist, load them
   * otherwise, create them with 0s
   *
   * @param dir directory where global counters are located
   */
  def readGlobalCounters(dir:String){
    val entitymention=GlobalCounter.readFromFile(dir+"/entitymention_count")
    val entityword=GlobalCounter.readFromFile(dir+"/entityword_count")
    val topicentity=GlobalCounter.readFromFile(dir+"/topicentity_count")
    Document.init(entitymention,entityword,topicentity,candMap,properties)

    val emFile=new File(dir+"/entitymention_sum")
    val ewFile=new File(dir+"/entityword_sum")
    val teFile=new File(dir+"/topicentity_sum")
    if(emFile.exists()&&ewFile.exists()&&teFile.exists()){
      entitymentionCounterSum=GlobalCounter.readFromFile(emFile.getAbsolutePath)
      entitywordCounterSum=GlobalCounter.readFromFile(ewFile.getAbsolutePath)
      topicentityCounterSum=GlobalCounter.readFromFile(teFile.getAbsolutePath)
    }else{
      entitymentionCounterSum=GlobalCounter("entitymention_sum", entitymention)
      entitywordCounterSum=GlobalCounter("entityword_sum",entityword)
      topicentityCounterSum=GlobalCounter("topicentity_sum",topicentity)
    }
  }

  /**
   * save counters omto disk
   *
   * @param dir directory to save counters
   * @param counters
   * @param avg if true, average the value of each entry by diving sample numbers.
   *            it should only be applied for aggregate counters
   */
  def saveGlobalCounters(dir:String,counters:List[GlobalCounter], avg:Boolean=false){
    createDir(dir)
    counters.foreach(counter=>{
      if(avg)
        counter.writeAvgToFile(dir+"/"+counter.name)
      else
        counter.writeToFile(dir+"/"+counter.name)
    })
  }

  /**
   * add counter values to aggregate counters
   * @param epoch
   */
  def updateCounterSum(epoch:Int){
    entitymentionCounterSum.add(Document.entitymentionCount)
    entitymentionCounterSum.samples+=1
    entitywordCounterSum.add(Document.entitywordCount)
    entitywordCounterSum.samples+=1
    topicentityCounterSum.add(Document.topicentityCount)
    topicentityCounterSum.samples+=1
  }


  /**
   * update document assignment and global counters through gibbs sampling
   * do sampling after burninEpoch; for every sampleLag epochs, do a sample (i.e., aggregate the counters)
   *
   * @param entityTopicFolder
   */
  def updateAssignments(entityTopicFolder:String, properties:Properties){
    val burninEpoch=properties.getProperty("burninEpoch").toInt
    val sampleLag=properties.getProperty("sampleLag").toInt
    val checkpointFreq=properties.getProperty("checkpointFreq").toInt
    val maxEpoch=properties.getProperty("maxEpoch").toInt
    val threadNum=properties.getProperty("threadNum").toInt

    val trainingDir=entityTopicFolder+"/traincorpus"
    val docCorpusList=new ListBuffer[DocumentCorpus]()
    //init document corpus list (all documents were split into a set of corpus, in initializeWikiDocuments())
    (0 until threadNum+1).foreach(id=>{
       val corpus=new DocumentCorpus("%s/%d".format(trainingDir, id))
       corpus.loadDocs()
       docCorpusList+=corpus
    })

    readGlobalCounters(trainingDir)
    (1 to maxEpoch).foreach((i:Int)=>{
      val memLoaded = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
      LOG.info("%d-th iteration, mem %d M".format(i, memLoaded))
      docCorpusList.foreach((corpus:DocumentCorpus)=>corpus.run)
      var finishedNum=0
      while(finishedNum<docCorpusList.size){
        Thread.sleep(60000)
        docCorpusList.foreach(corpus=>if(corpus.isRunning==false) finishedNum+=1)
      }
      if(i>=burninEpoch && i%sampleLag==0)
        updateCounterSum(i)

      if(i>=burninEpoch&&i%checkpointFreq==0){
        docCorpusList.foreach(corpus=>corpus.dump())
        saveGlobalCounters(trainingDir,
          List(Document.entitymentionCount,Document.topicentityCount, Document.entitywordCount,
            entitymentionCounterSum, entitywordCounterSum, topicentityCounterSum))
        doCheckpoint(i,entityTopicFolder)
      }
    })
  }

  /**
   *
   * 1. use spotlight model to extract dbpedia resource occrs from plain text
   * 2. correct miss-recognized resource occrs according wiki links if doCorrection=true
   *
   * @param pageNode
   * @param text
   * @return dbpedia resource occrs sorted based on textoffset
   */

  def extractDbpeidaResourceOccurrenceFromWikiPage(pageNode:PageNode, text:Text, doCorrection:Boolean=false):List[DBpediaResourceOccurrence]={
    // exclude redirect and disambiguation pages
    if (!pageNode.isRedirect && !pageNode.isDisambiguation) {
      try{
        val sfOccrs=spotter.extract(text)
        val bestK=disambiguator.bestK(new Paragraph(text,sfOccrs.toList), 1)
        if(doCorrection){
          val idBase = pageNode.title.encoded+"-p"
          val groundResOccrs=WikiOccurrenceSource.getOccurrences(pageNode.children, idBase).map(occr=>{
            occr.resource.uri = wikiToDBpediaClosure.wikipediaToDBpediaURI(occr.resource.uri)
            try{
              occr.resource.id=searcher.resStore.getResourceByName(occr.resource.uri).id
              occr.surfaceForm.id=searcher.sfStore.getSurfaceForm(occr.surfaceForm.name).id
              Option(occr)
            }catch {
              case e:DBpediaResourceNotFoundException=>None
              case e:SurfaceFormNotFoundException=>None
            }
          }).filter(a=>a.nonEmpty).flatten.toList
          groundResOccrs.foreach(resoccr=>{
            val l=List[DBpediaResourceOccurrence](resoccr)
            bestK.put(Factory.SurfaceFormOccurrence.from(resoccr), l)
          })
        }
        bestK.values.filter(a=>a.size>0).flatten.toList.sortWith((a,b)=>a.textOffset<b.textOffset)
      }catch {
        case e:SpottingException=>{
          System.out.println("sentence too long for "+pageNode.title)
          List[DBpediaResourceOccurrence]()
          }
      }
    }
    else
      List[DBpediaResourceOccurrence]()
  }

  /**
   * parse raw wiki dump, init a doc instance for each wiki page
   * multiple DocmentInitializers works parallelly
   * each DocumentInitializer has a DocumentCorpus to save itermediate results
   *
   * @param wikidump
   * @param entityTopicFolder
   */
  def initializeWikiDocuments(wikidump:String, entityTopicFolder: String)={
    val threadNum=properties.getProperty("threadNum").toInt
    createDir(entityTopicFolder)
    val trainingDir=entityTopicFolder+"/traincorpus/"
    createDir(trainingDir)
    val initializers=(0 until threadNum+1).map((k:Int)=>{
      val docStore=trainingDir+k.toString()
      DocumentInitializer(properties,docStore, true)
    })

    val pool=Executors.newFixedThreadPool(threadNum)
    var parsedDocs=0


    val wikisource=XMLSource.fromFile(new File(wikidump), Language("nl"))

    //iterate each wiki page
    wikisource.foreach((wikiPage:WikiPage)=>{
      System.out.println("to parse:"+wikiPage.title)
      // clean the wiki markup from everything but links
      val cleanSource = WikiMarkupStripper.stripEverything(wikiPage.source)
      // parse the (clean) wiki page
      val pageNode = wikiParser( WikiPageUtil.copyWikiPage(wikiPage, cleanSource) )
      val text=new Text(pageNode.toPlainText)
      tokenizer.tokenizeMaybe(text)
      val resOccrs=extractDbpeidaResourceOccurrenceFromWikiPage(pageNode,text)
      if(resOccrs.size>0){
        var idleInitializer=None.asInstanceOf[Option[DocumentInitializer]]
        while(idleInitializer==None)
          idleInitializer=initializers.find((initializer:DocumentInitializer)=>initializer.isRunning==false)
        val runner=idleInitializer.get
        runner.set(text, resOccrs.toArray)
        pool.execute(runner)

        parsedDocs+=1
        if(parsedDocs%1000==0){
          val memLoaded = (Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()) / (1024 * 1024)
          LOG.info("%d docs parsed, mem %d M".format(parsedDocs,  memLoaded))
        }
      }
    })

    LOG.info("%d docs parsed".format(parsedDocs))
    shutdownAndAwaitTermination(pool)
    val counters=mergeCounters(initializers.toArray)
    saveGlobalCounters(trainingDir,counters)
    //save the init corpus and counters
    doCheckpoint(0,entityTopicFolder)
  }

  /**
   * merge counters from all initializers
   *
   * @param initializers
   * @return
   */
  def mergeCounters(initializers:Array[DocumentInitializer]):List[GlobalCounter]={
    val ret=initializers(0)
    ret.docCorpus.closeOutputStream()
    (1 until initializers.length).foreach((i:Int)=>{
      ret.topicentityCount.add(initializers(i).topicentityCount)
      ret.entitymentionCount.add(initializers(i).entitymentionCount)
      ret.entitywordCount.add(initializers(i).entitywordCount)
      initializers(i).docCorpus.closeOutputStream()
    })

    List(ret.topicentityCount,ret.entitymentionCount,ret.entitywordCount)
  }

  def shutdownAndAwaitTermination(pool:ExecutorService) {
    pool.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(300, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(300, TimeUnit.SECONDS))
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


  /**
   * Entry of training function
   *
   * @param wikidump path to wiki dump(xml) file
   * @param entityTopicFolder path to entitytopic directory, which is under the spotlight model folder, named 'entitytopic'
   * @param parseWiki parse wikidump if set to true; otherwise, be sure that the wikidump has already been parsed before
   *                  i.e., a 'traincorpus' which contains initialized document corpus and counter files should exist
   */
  def learnFromWiki(wikidump:String, entityTopicFolder:String, parseWiki:Boolean){
    if(parseWiki){
      //wikidump has not been parsed yet
      LOG.info("Init wiki docs...")
      val start = System.currentTimeMillis()
      initializeWikiDocuments(wikidump, entityTopicFolder)
      LOG.info("Done (%d ms)".format(System.currentTimeMillis() - start))
    }

    LOG.info("Update assignments...")
    val start=System.currentTimeMillis()
    updateAssignments(entityTopicFolder, properties)
    LOG.info("Done (%d ms)".format(System.currentTimeMillis() - start))

    //save global knowledge/counters
    saveGlobalCounters(entityTopicFolder, List(entitymentionCounterSum,entitywordCounterSum,topicentityCounterSum), true)
    LOG.info("Finish training")
  }
}


object TrainEntityTopicDisambiguator{

  /**
   * prepare stores, etc. for training entitytopic disambiguator
   * @param modelFolder spotlight model folder
   * @return
   */
  def fromFolder(modelFolder: File): TrainEntityTopicDisambiguator = {
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

    val (tokenTypeStore, sfStore, resStore, candMapStore, contextStore) = SpotlightModel.storesFromFolder(modelFolder)

    val topicNum=properties.getProperty("topicNum").toInt
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

    val disambigName= properties.getProperty("predisambiguator")
    val disambiguator = if (disambigName.indexOf("baseline")>=0)
      new DBBaselineDisambiguator(sfStore, resStore, candMapStore)
    else
      new DBTwoStepDisambiguator(
        tokenTypeStore,
        sfStore,
        resStore,
        searcher,
        contextStore,
        new UnweightedMixture(Set("P(e)", "P(c|e)", "P(s|e)")),
        new GenerativeContextSimilarity(tokenTypeStore)
      )

    val spotter=SpotlightModel.createSpotter(modelFolder,sfStore,stopwords,cores)
    new TrainEntityTopicDisambiguator(wikipediaToDBpediaClosure,tokenizer, searcher, candMapStore.asInstanceOf[MemoryCandidateMapStore],
      disambiguator, spotter, properties)
  }

  def main(args:Array[String]){
    var spotlightmodel_path="model"
    var wikidump_path="data"
    var init=false

    var i=0
    System.out.println("total arg num "+args.length)
    while(i<args.length){
      System.out.println(args(i))
      if(args(i)=="-spotlight"){
        spotlightmodel_path=args(i+1)
        i+=2
      }else if(args(i)=="-data"){
        wikidump_path=args(i+1)
        i+=2
      }else if(args(i)=="-init"){
        init=true
        i+=1
      }else{
        i+1
      }
    }

    val trainer:TrainEntityTopicDisambiguator=TrainEntityTopicDisambiguator.fromFolder(new File(spotlightmodel_path))
    trainer.learnFromWiki(wikidump_path, spotlightmodel_path+"/entitytopic/", init)
  }
}
