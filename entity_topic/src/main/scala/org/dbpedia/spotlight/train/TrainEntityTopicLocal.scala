package org.dbpedia.spotlight.train

import org.apache.commons.logging.LogFactory
import com.esotericsoftware.kryo.Kryo
import java.io.{FileOutputStream, FileInputStream, File}
import java.util.Locale
import org.dbpedia.spotlight.db.{FSASpotter, SpotlightModel, WikipediaToDBpediaClosure}
import org.dbpedia.spotlight.db.memory.{MemoryResourceStore, MemoryStore, MemoryCandidateMapStore, MemorySurfaceFormStore}
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.db.tokenize.LanguageIndependentTokenizer
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import akka.actor._
import akka.pattern._
import org.dbpedia.spotlight.io.{WikiOccurrenceSource, AllOccurrenceSource, EntityTopicDocument}
import com.esotericsoftware.kryo.io.{Input, Output}
import org.dbpedia.extraction.util.Language
import org.dbpedia.spotlight.model._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.util.Timeout
import akka.routing.{Broadcast, BalancingPool, SmallestMailboxRouter, RoundRobinRouter}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, NotADBpediaResourceException, DBpediaResourceNotFoundException}
import scala.Some
import org.dbpedia.spotlight.db.model.{SurfaceFormStore, ResourceStore, TextTokenizer}
import java.util.concurrent.TimeUnit


/**
 * @author dirk
 *         Date: 4/11/14
 *         Time: 4:49 PM
 */
object TrainEntityTopicLocal {

  private val LOG = LogFactory.getLog(getClass)

  private[train] def newKryo = {
    val kryo = new Kryo()
    kryo.register(classOf[Array[EntityTopicDocument]])
    kryo
  }

  def main(args: Array[String]) {
    val kryo = newKryo

    val wikidump = new File(args(0))
    val modelDir = new File(args(1))

    val numTopics = args(2).toInt
    val dataDir = new File(args(3))

    val parallelism = if (args.size > 4) args(4).toInt else Runtime.getRuntime.availableProcessors()

    val cacheSize = 100000

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

    val (tokenStore, sfStore, resStore, candMapStore, _) = SpotlightModel.storesFromFolder(modelDir)
    val vocabSize = tokenStore.getVocabularySize
    val numMentions = sfStore.asInstanceOf[MemorySurfaceFormStore].size
    val numEntities = resStore.asInstanceOf[MemoryResourceStore].size

    val candMap = candMapStore.asInstanceOf[MemoryCandidateMapStore]

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

    val model = new SimpleEntityTopicModel(numTopics, numEntities, vocabSize, numMentions, alpha, beta, gamma, delta, candMap)

    dir = new File(dataDir, s"etd_0")
    dir.mkdirs()
    dir.listFiles().foreach(_.delete())

    implicit val system = ActorSystem("ET")

    val writer = system.actorOf(Props(classOf[WriterActor],cacheSize, kryo))
    var sampler = system.actorOf(BalancingPool(math.max(1, parallelism / 3 * 2 - 1)).props(Props(classOf[SamplerActor], model, writer)))
    val parser = system.actorOf(BalancingPool(math.max(1, parallelism / 3 - 1)).props(Props(classOf[ParserActor], sampler, spotter, tokenizer,
      resStore, sfStore, candMapStore.asInstanceOf[MemoryCandidateMapStore].candidates,
      wikipediaToDBpediaClosure)))

    implicit val timeout =  Timeout(10,TimeUnit.HOURS)

    var ctr = 0
    val occSource = WikiOccurrenceSource.fromXMLDumpFile(wikidump, Language.English)

    var currentContext: Text = new Text("")
    var currentAnnotations = List[DBpediaResourceOccurrence]()
    occSource.foreach(resOcc => {
      if (currentContext == resOcc.context) currentAnnotations ::= resOcc
      else {
        if (currentContext.text != "" && !currentAnnotations.isEmpty) {
          parser !(currentContext, currentAnnotations)
          ctr += 1
          if (ctr % cacheSize == 0) {
            sampler ! ctr
            parser ! ctr
            LOG.info(s"$ctr entity topic documents pushed")
          }
        }

        currentContext = resOcc.context
        currentAnnotations = List(resOcc)
      }
    })
    parser !(currentContext, currentAnnotations)

    parser ! Broadcast(PoisonPill)
    sampler ! Broadcast(PoisonPill)

    while(!parser.isTerminated || !sampler.isTerminated)
      Thread.sleep(1000) //check each second
    Await.result(writer ? "write and reset",Duration.Inf)

    sampler = system.actorOf(BalancingPool(math.max(1, parallelism - 2)).props(Props(classOf[SamplerActor], model, writer)))

    SimpleEntityTopicModel.toFile(new File(dataDir, "model0"), model)
    init = false
    (1 until 500).foreach(i => {
      val oldDir = dir
      dir = new File(dataDir, s"etd_$i")
      dir.mkdirs()
      dir.listFiles().foreach(_.delete())
      ctr = 0

      oldDir.listFiles().foreach(file => {
        val in = new Input(new FileInputStream(file))
        val docs = kryo.readObject(in, classOf[Array[EntityTopicDocument]])
        docs.foreach(doc => {
          sampler ! doc
          ctr += 1
          if (ctr % cacheSize == 0) {
            sampler ! ctr
            LOG.info(ctr + " entity-topic-documents pushed")
          }
        })
      })
      sampler ! Broadcast(PoisonPill)
      while(!sampler.isTerminated)
        Thread.sleep(1000) //check each second
      sampler = system.actorOf(BalancingPool(math.max(1, parallelism - 2)).props(Props(classOf[SamplerActor], model, writer)))
      Await.result(writer ? "write and reset",Duration.Inf)

      SimpleEntityTopicModel.toFile(new File(dataDir, s"model$i"), model)
    })


    System.exit(0)
  }

  var init = true
  var dir: File = null

  private class SamplerActor(model: SimpleEntityTopicModel, writer: ActorRef) extends Actor {
    var counter = 0

    def receive = {
      case doc: EntityTopicDocument =>
        try {
          model.gibbsSampleDocument(doc, withUpdate = true, init)
          writer ! doc
        }
        catch {
          case e: Throwable => LOG.error("Error sampling a document! But don't worry the party goes on without it!\n" + doc.toString + "\n" + e.printStackTrace())
        }
      case counter: Int => LOG.info(counter + " entity-topic-documents sampled")
    }
  }

  private class WriterActor(cacheSize: Int, kryo: Kryo) extends Actor {
    var cached = new Array[EntityTopicDocument](cacheSize)
    var currentCacheSize = 0
    var counter = 0

    def receive = {
      case doc: EntityTopicDocument =>
        cached(currentCacheSize) = doc
        currentCacheSize += 1
        counter += 1
        if (currentCacheSize == cacheSize) {
          writeDocuments(new File(dir, "etd_" + counter), cached, kryo)
          currentCacheSize = 0
          LOG.info(counter + " entity-topic-documents written")
        }
      case "write and reset" =>
        writeDocuments(new File(dir, "etd_" + counter), cached.take(currentCacheSize), kryo)
        LOG.info(counter + " entity-topic-documents written")
        currentCacheSize = 0
        counter = 0
        sender ! "done"
    }
  }

  private def writeDocuments(file: File, docs: Array[EntityTopicDocument], kryo: Kryo) {
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
        case ex: DBpediaResourceNotFoundException => LOG.debug(ex.getMessage)
        case ex: NotADBpediaResourceException => LOG.debug(e.uri + " -> " + ex.getMessage)
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
              case ex: SurfaceFormNotFoundException => LOG.debug(ex.getMessage)
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

      val document = EntityTopicDocument(
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
          case e: Throwable => LOG.error("Error parsing the following text! But don't worry the party goes on without it!\n" + currentContext.text + "\n" + e.printStackTrace())
        }
      case counter: Int => LOG.info(counter + " entity-topic-documents parsed")
    }
  }

}
