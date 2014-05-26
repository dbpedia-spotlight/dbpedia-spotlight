package org.dbpedia.spotlight.train

import com.esotericsoftware.kryo.Kryo
import java.io.{FileOutputStream, FileInputStream, File}
import java.util.Locale
import org.dbpedia.spotlight.db.{FSASpotter, SpotlightModel, WikipediaToDBpediaClosure}
import org.dbpedia.spotlight.db.memory._
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.db.tokenize.LanguageIndependentTokenizer
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import akka.actor._
import akka.pattern._
import org.dbpedia.spotlight.io.{WikiOccurrenceSource}
import com.esotericsoftware.kryo.io.{Input, Output}
import org.dbpedia.extraction.util.Language
import org.dbpedia.spotlight.model._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.util.Timeout
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, NotADBpediaResourceException, DBpediaResourceNotFoundException}
import org.dbpedia.spotlight.db.model.{SurfaceFormStore, ResourceStore, TextTokenizer}
import java.util.concurrent.TimeUnit
import scala.Some
import akka.routing.Broadcast
import akka.routing.SmallestMailboxPool
import org.dbpedia.spotlight.log.SpotlightLog

/**
 * Trains the entity topic model. Command line args: wiki-dump.xml spotlight-model-dir numTopics(e.g. 300) outputDir parallelism(optional).
 * It is an actor based implementation and makes use of local parallelism, but IO is the bottle neck, so decent parallelism (e.g. 8) should be enough.
 * Make sure that the model folder contains "redirects.nt" and "disambiguations.nt", and rename "context.mem" to something else, using it does not help and
 * requires additional RAM.
 */
object TrainEntityTopicLocal {

  private[train] def newKryo = {
    val kryo = new Kryo()
    kryo.register(classOf[Array[EntityTopicTrainingDocument]])
    kryo
  }

  def main(args: Array[String]) {
    val kryo = newKryo

    val dump = new File(args(0))
    val modelDir = new File(args(1))

    val numTopics = args(2).toInt
    val dataDir = new File(args(3))
    val samplingIterations = 1

    val parallelism = if (args.size > 4) args(4).toInt else Runtime.getRuntime.availableProcessors()

    val cacheSize = 100000

    var model:SimpleEntityTopicModel = null

    var ctr = 0

    implicit val system = ActorSystem("ET")
    implicit val timeout =  Timeout(10,TimeUnit.HOURS)
    var sampler:ActorRef = null
    val writer = system.actorOf(Props(classOf[WriterActor],cacheSize, kryo))

    if(!dump.isDirectory) {
      //currently only en
      val locale = new Locale("en", "US")
      val namespace = if (locale.getLanguage.equals("en")) {
        "http://dbpedia.org/resource/"
      } else {
        "http://%s.dbpedia.org/resource/"
      }

      val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
        namespace,
        new FileInputStream(new File(modelDir, "redirects.nt")),
        new FileInputStream(new File(modelDir, "disambiguations.nt"))
      )

      val (tokenStore, sfStore, resStore, candMapStore, contextStore) = SpotlightModel.storesFromFolder(modelDir)
      val vocabSize = tokenStore.getVocabularySize
      val numMentions = sfStore.asInstanceOf[MemorySurfaceFormStore].size
      val numEntities = resStore.asInstanceOf[MemoryResourceStore].size

      val candMap = candMapStore.asInstanceOf[MemoryCandidateMapStore]
      val memoryContextStore = if(contextStore != null) contextStore.asInstanceOf[MemoryContextStore] else null

      val beta = 0.1
      val gamma = 0.0001
      val delta = 2000.0 / vocabSize
      val alpha = 50.0 / numTopics

      val stopwords: Set[String] = SpotlightModel.loadStopwords(modelDir)

      lazy val spotter = {
        val dict = MemoryStore.loadFSADictionary(new FileInputStream(new File(modelDir, "fsa_dict.mem")))
        new FSASpotter(
          dict,
          sfStore,
          Some(SpotlightModel.loadSpotterThresholds(new File(modelDir, "spotter_thresholds.txt"))),
          stopwords
        ).asInstanceOf[Spotter]
      }

      val tokenizer =
        new LanguageIndependentTokenizer(stopwords, new SnowballStemmer("EnglishStemmer"), locale, tokenStore)

      model = new SimpleEntityTopicModel(numTopics, numEntities, vocabSize, numMentions, alpha, beta, gamma, delta,
        create=true, candMap, memoryContextStore)

      dir = new File(dataDir, s"etd_0")
      dir.mkdirs()
      dir.listFiles().foreach(_.delete())

      sampler = system.actorOf(SmallestMailboxPool(math.max(1, parallelism / 3 * 2 - 1)).props(Props(classOf[SamplerActor], model, writer,samplingIterations)))
      val parser = system.actorOf(SmallestMailboxPool(math.max(1, parallelism / 3 - 1)).props(Props(classOf[ParserActor], sampler, spotter, tokenizer,
        resStore, sfStore, candMapStore.asInstanceOf[MemoryCandidateMapStore].candidates,
        wikipediaToDBpediaClosure)))

      val occSource = WikiOccurrenceSource.fromXMLDumpFile(dump, Language.English)

      var currentContext: Text = new Text("")
      var currentAnnotations = List[DBpediaResourceOccurrence]()
      occSource.foreach(resOcc => {
        if (currentContext == resOcc.context) currentAnnotations ::= resOcc
        else {
          if (currentContext.text != "" && !currentAnnotations.isEmpty) {
            parser ! (currentContext, currentAnnotations)
            ctr += 1
            if (ctr % cacheSize == 0) {
              parser ! ctr
              SpotlightLog.info(getClass,s"$ctr entity topic documents pushed")
            }
          }

          currentContext = resOcc.context
          currentAnnotations = List(resOcc)
        }
      })
      parser !(currentContext, currentAnnotations)

      parser ! Broadcast(PoisonPill.getInstance)

      while(!parser.isTerminated )
        Thread.sleep(1000) //check each second

      sampler ! Broadcast(PoisonPill.getInstance)
      while(!sampler.isTerminated)
        Thread.sleep(1000)

      Await.result(writer ? "write and reset",Duration.Inf)
      SimpleEntityTopicModel.toFile(new File(dataDir, "model0"), model)
    }
    else {
      dir = dump
      //load latest model
      model = SimpleEntityTopicModel.fromFile(dataDir.listFiles().filter(f => f.isFile && f.getName.contains("model")).sortBy(-_.lastModified()).head)

      //Add candidate-map to model for training
      val modelDataFolder = new File(modelDir, "model")
      val quantizedCountsStore = MemoryStore.loadQuantizedCountStore(new FileInputStream(new File(modelDataFolder, "quantized_counts.mem")))
      val resStore = MemoryStore.loadResourceStore(new FileInputStream(new File(modelDataFolder, "res.mem")), quantizedCountsStore)
      val candMapStore = MemoryStore.loadCandidateMapStore(new FileInputStream(new File(modelDataFolder, "candmap.mem")), resStore, quantizedCountsStore)

      model.candMap = candMapStore
    }


    //Create new samplers
    sampler = system.actorOf(SmallestMailboxPool(math.max(1, parallelism - 2)).props(Props(classOf[SamplerActor], model, writer,samplingIterations)))

    init = false
    SpotlightLog.info(getClass,"Finished initial iteration!")

    (1 until 500).foreach(i => {
      val oldDir = dir
      dir = new File(dataDir, s"etd_$i")
      dir.mkdirs()
      dir.listFiles().foreach(_.delete())
      ctr = 0

      oldDir.listFiles().foreach(file => {
        val docs = loadDocs(file, kryo)
        docs.foreach(doc => {
          sampler ! doc
          ctr += 1
          if (ctr % cacheSize == 0) {
            sampler ! ctr
            SpotlightLog.info(getClass,ctr + " entity-topic-documents pushed")
          }
        })
        if(i > 1)
          file.delete()
      })
      sampler ! Broadcast(PoisonPill)
      while(!sampler.isTerminated)
        Thread.sleep(1000) //check each second
      sampler = system.actorOf(SmallestMailboxPool(math.max(1, parallelism - 2)).props(Props(classOf[SamplerActor], model, writer, samplingIterations)))
      Await.result(writer ? "write and reset",Duration.Inf)

      SimpleEntityTopicModel.toFile(new File(dataDir, s"model$i"), model)
      if(i > 1) { // keep model of first iteration
        new File(dataDir, s"model${i-1}").delete()
        oldDir.delete()
      }
      SpotlightLog.info(getClass,s"Finished iteration $i!")
    })


    System.exit(0)
  }


  def loadDocs(file: File, kryo: Kryo)  = {
    val in = new Input(new FileInputStream(file))
    val docs = kryo.readObject(in, classOf[Array[EntityTopicTrainingDocument]])
    in.close()
    docs
  }

  var init = true
  var dir: File = null

  private class SamplerActor(model: SimpleEntityTopicModel, writer: ActorRef, iterations:Int) extends Actor {
    var counter = 0

    def receive = {
      case doc: EntityTopicTrainingDocument =>
        try {
          model.trainWithDocument(doc, firstTime = init)
          writer ! doc
        }
        catch {
          case e: Throwable => println(e.printStackTrace()); SpotlightLog.error(getClass,"Error sampling a document! But don't worry the party goes on without it!")
        }
      case counter: Int => SpotlightLog.info(getClass,counter + " entity-topic-documents sampled")
    }
  }

  private class WriterActor(cacheSize: Int, kryo: Kryo) extends Actor {
    var cached = new Array[EntityTopicTrainingDocument](cacheSize)
    var currentCacheSize = 0
    var counter = 0

    def receive = {
      case doc: EntityTopicTrainingDocument =>
        cached(currentCacheSize) = doc
        currentCacheSize += 1
        counter += 1
        if (currentCacheSize == cacheSize) {
          writeDocuments(new File(dir, "etd_" + counter), cached, kryo)
          currentCacheSize = 0
          SpotlightLog.info(getClass,counter + " entity-topic-documents written")
        }
      case "write and reset" =>
        writeDocuments(new File(dir, "etd_" + counter), cached.take(currentCacheSize), kryo)
        SpotlightLog.info(getClass,counter + " entity-topic-documents written")
        currentCacheSize = 0
        counter = 0
        sender ! "done"
    }
  }

  private def writeDocuments(file: File, docs: Array[EntityTopicTrainingDocument], kryo: Kryo) {
    val out = new Output(new FileOutputStream(file))
    kryo.writeObject(out, docs)
    out.close()
  }

  private class ParserActor(sampler: ActorRef, spotter: Spotter,
                            tokenizer: TextTokenizer,
                            resStore: ResourceStore,
                            sfStore: SurfaceFormStore,
                            candidates: Array[Array[Int]],
                            wikiToDBpediaClosure: WikipediaToDBpediaClosure) extends Actor {

    def getResourceId(e: DBpediaResource) = {
      var id = Int.MinValue
      try {
        val uri = wikiToDBpediaClosure.wikipediaToDBpediaURI(e.uri)
        id = resStore.getResourceByName(uri).id
      }
      catch {
        case ex: DBpediaResourceNotFoundException => SpotlightLog.debug(getClass,ex.getMessage)
        case ex: NotADBpediaResourceException => SpotlightLog.debug(getClass,e.uri + " -> " + ex.getMessage)
      }
      id
    }

    def getDocument(currentContext: Text, currentAnnotations: List[DBpediaResourceOccurrence]) = {
      val tokens = tokenizer.tokenize(currentContext)
      currentContext.setFeature(new Feature("tokens", tokens))

      val spots = spotter.extract(currentContext)

      val anchors =
        currentAnnotations.foldLeft((List[Int](), List[Int]())) {
          case ((resourceIds, sfIds), occ) =>
            var id = 0
            try {
              id = sfStore.getSurfaceForm(occ.surfaceForm.name).id
            }
            catch {
              case ex: SurfaceFormNotFoundException => SpotlightLog.debug(getClass,ex.getMessage)
            }
            if (id > 0 && candidates(id) != null && candidates(id).length > 0)
              (getResourceId(occ.resource) :: resourceIds, id :: sfIds)
            else (resourceIds, sfIds)
        }

      val (resources, mentions) = spots.foldLeft(anchors) {
        case ((resourceIds, sfIds), spot) =>
          val id = spot.surfaceForm.id
          if (id > 0 && candidates(id) != null && candidates(id).length > 0 && !currentAnnotations.exists(_.textOffset == spot.textOffset))
            (Int.MinValue :: resourceIds, id :: sfIds)
          else (resourceIds, sfIds)
      }

      val mentionEntities = resources.toArray
      val tokenArray = tokens.filter(_.tokenType.id > 0).toArray

      val document = new EntityTopicTrainingDocument(
        tokenArray.map(_.tokenType.id),
        tokenArray.map(_ => Int.MinValue),
        mentions.toArray,
        mentionEntities,
        mentionEntities.map(_ => Int.MinValue))

      document
    }

    def receive = {
      case (currentContext: Text, currentAnnotations: List[DBpediaResourceOccurrence]) =>
        try {
          val doc = getDocument(currentContext, currentAnnotations)
          sampler ! doc
        }
        catch {
          case e: Throwable => println(e.printStackTrace()); SpotlightLog.error(getClass,"Error parsing the following text! But don't worry the party goes on without it!\n" + currentContext.text)
        }
      case counter: Int => SpotlightLog.info(getClass,counter + " entity-topic-documents parsed")
    }
  }

}
