package org.dbpedia.spotlight.db.entitytopic

import java.io.{FileOutputStream, FileInputStream, File}
import org.dbpedia.spotlight.db.memory._
import java.util.{Locale, Properties}
import org.dbpedia.spotlight.db.model.TextTokenizer
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.db.{DBTwoStepDisambiguator, SpotlightModel, DBCandidateSearcher, WikipediaToDBpediaClosure}
import scala.collection.mutable.ListBuffer
import org.apache.commons.logging.LogFactory
import java.util.concurrent.{ExecutorService, TimeUnit, Executors}
import org.apache.commons.io.FileUtils
import org.dbpedia.extraction.wikiparser.{PageNode, WikiParser}
import org.dbpedia.extraction.sources.{WikiPage, XMLSource}
import org.dbpedia.extraction.util.Language
import org.dbpedia.spotlight.string.WikiMarkupStripper
import org.dbpedia.spotlight.io.{WikiOccurrenceSource, WikiPageUtil}
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ
import org.dbpedia.spotlight.disambiguate.mixtures.UnweightedMixture
import org.dbpedia.spotlight.db.similarity.GenerativeContextSimilarity
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.model.Paragraph


/**
 * Learn global knowledge/counters from wikipeida dump accroding to entity topic model
 * ref: Xianpei Han, Le Sun: An Entity-Topic Model for Entity Linking. EMNLP-CoNLL 2012: 105-115
 *
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
                        val disambiguator:ParagraphDisambiguatorJ,
                        val spotter: Spotter,
                        val properties: Properties
                        )  {
  val LOG = LogFactory.getLog(this.getClass)

  var entitymentionCounterSum:GlobalCounter=null
  var entitywordCounterSum:GlobalCounter=null
  var topicentityCounterSum:GlobalCounter=null

  val wikiParser = WikiParser()

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
   * load gloal counters into memory
   * if aggregate counter(ending with _sum) files exist, load them
   * otherwise, create them with empty content
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
   * save counters into disk
   *
   * @param dir
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
   * add counter values to aggregate counter
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
   *
   * @param entityTopicFolder
   */
  def updateAssignments(entityTopicFolder:String, properties:Properties){
    val burninEpoch=properties.getProperty("burninEpoch").toInt
    // sample/aggregate every samplelag epoch
    val sampleLag=properties.getProperty("sampleLag").toInt
    val checkpointFreq=properties.getProperty("checkpointFreq").toInt
    val maxEpoch=properties.getProperty("maxEpoch").toInt

    val trainingDir=entityTopicFolder+"/traincorpus"
    val docCorpusList=new ListBuffer[DocumentCorpus]()
    new File(trainingDir).listFiles().foreach((c:File)=>{
      if (c.getName.matches("[0-9]+.+"))
        docCorpusList+=new DocumentCorpus(c.getPath.substring(0,c.getPath.indexOf('.')))
    })

    ( 1 to maxEpoch).foreach((i:Int)=>{
      var j:Int=0
      readGlobalCounters(trainingDir)
      docCorpusList.foreach((corpus:DocumentCorpus)=>{
        val documents=corpus.loadDocs()
        documents.foreach((doc:Document)=>{
          doc.updateAssignment(true)
          corpus.add(doc)
          j+=1
          if(j%10000==0)
            LOG.info("%d of %d-th iteration".format(j, i))
        })
        corpus.closeOutputStream()

        if(i>=burninEpoch && i%sampleLag==0)
          updateCounterSum(i)

        //save counters for every epoch to re-organze the hash map
        saveGlobalCounters(trainingDir,
          List(Document.entitymentionCount,Document.topicentityCount, Document.entitywordCount,
            entitymentionCounterSum, entitywordCounterSum, topicentityCounterSum))

        if(i>=burninEpoch&&i%checkpointFreq==0){
          doCheckpoint(i,entityTopicFolder)
        }
      })
    })
  }

  var parsedRes=0
  var unknownSF=0
  var unknownRes=0
  var emptyCands=0

  /**
   * 1. parse wiki page, extract dbpedia resource occrs from links
   * 2. use spotlight model to extract dbpedia resource occrs from plain text
   * 3. correct miss-recognized links
   *
   * @param pageNode
   * @param text
   * @return dbpedia resource occrs sorted based on textoffset
   */

  def extractDbpeidaResourceOccurrenceFromWikiPage(pageNode:PageNode, text:Text):List[DBpediaResourceOccurrence]={
    // exclude redirect and disambiguation pages
    if (!pageNode.isRedirect && !pageNode.isDisambiguation) {
      val idBase = pageNode.title.encoded+"-p"
      val groundResOccrs=WikiOccurrenceSource.getOccurrences(pageNode.children, idBase).map(occr=>{
        occr.resource.uri = wikiToDBpediaClosure.wikipediaToDBpediaURI(occr.resource.uri)
        occr.resource.id=searcher.resStore.getResourceByName(occr.resource.uri).id
        occr
      }).toList

      val sfOccrs=spotter.extract(text)
      val bestK=disambiguator.bestK(new Paragraph(text,sfOccrs.toList), 1)
      groundResOccrs.foreach(resoccr=>{
        val l=List[DBpediaResourceOccurrence](resoccr)
        bestK.put(Factory.SurfaceFormOccurrence.from(resoccr), l)
      })
      bestK.values().toList(0).sortWith((a,b)=>a.textOffset<b.textOffset).toList
    }
    else
      List[DBpediaResourceOccurrence]()
  }

  /**
   * parse raw wiki dump, init a doc instance for each wiki page
   * multiple DocmentInitializers works parallelly
   * each DocumentInitializer has a docStore file to save itermediate results
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
    wikisource.foreach((wikiPage:WikiPage)=>{
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
          LOG.info("%d docs parsed, parsed res %d unknown sf %d, unknown res %d, empty cands %d, mem %d M".format(
            parsedDocs, parsedRes, unknownSF, unknownRes, emptyCands, memLoaded))
        }
      }
    })

    shutdownAndAwaitTermination(pool)
    val counters=mergeCounters(initializers.toArray)
    saveGlobalCounters(trainingDir,counters)
    //save the init corpus and counters
    doCheckpoint(0,entityTopicFolder)
  }

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
      //wikidump has not been parsed
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

    val disambiguator = new ParagraphDisambiguatorJ(new DBTwoStepDisambiguator(
      tokenTypeStore,
      sfStore,
      resStore,
      searcher,
      contextStore,
      new UnweightedMixture(Set("P(e)", "P(c|e)", "P(s|e)")),
      new GenerativeContextSimilarity(tokenTypeStore)
    ))

    val spotter=SpotlightModel.createSpotter(modelFolder,sfStore,stopwords,cores)
    new TrainEntityTopicDisambiguator(wikipediaToDBpediaClosure,tokenizer, searcher, candMapStore.asInstanceOf[MemoryCandidateMapStore],
      disambiguator, spotter, properties)
  }

  def main(args:Array[String]){
    var spotlightmodel_path="model"
    var wikidump_path="data"
    var init=false

    var i=0
    while(i<args.length){
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