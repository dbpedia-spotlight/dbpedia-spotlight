package org.dbpedia.spotlight.train

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.SparkContext._
import org.apache.spark.streaming.{Seconds}
import org.dbpedia.spotlight.io.{AllOccurrenceSource, EntityTopicModelDocumentsSource}
import org.dbpedia.extraction.util.Language
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.db.{FSASpotter, SpotlightModel, WikipediaToDBpediaClosure}
import org.dbpedia.spotlight.db.memory._
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.db.tokenize.LanguageIndependentTokenizer
import java.util.{Locale}
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import scala.collection.mutable
import org.dbpedia.spotlight.storage.{RedisCountStore, LocalCountStore, CountStoreConfiguration, CountStore}
import scala.Predef._
import com.esotericsoftware.kryo.Kryo
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.StreamingContext
import org.dbpedia.spotlight.io.EntityTopicDocument
import org.apache.spark.serializer.KryoRegistrator
import com.esotericsoftware.kryo.io.{Input, Output}
import org.apache.commons.logging.LogFactory


/**
 * Created by dirkw on 3/11/14.
 */
object TrainEntityTopicModelWithSpark {

  private val LOG = LogFactory.getLog(getClass)

  def main(args: Array[String]) {
    System.setProperty("spark.cleaner.ttl", "600000")
    val kryo = new Kryo()
    new EntityTopicKryoRegistrator().registerClasses(kryo)

    val wikidump = new File(args(0))
    val modelDir = new File(args(1))
    val sparkCluster = args(2)
    val storeHost = args(3)
    val storePort = args(4).toInt

    val storeConf = new CountStoreConfiguration()
    storeConf.setHost(storeHost)
    storeConf.setPort(storePort)

    val numTopics = args(5).toInt
    val dataDir = new File(args(6))
    val parallelism = if (args.size > 7) args(7).toInt else 8
    val nrOfDocsInRDD = if (args.size > 8) args(8).toInt else 1000

    val storeName = "entity_topic_model_store"

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
    val mentionSize = sfStore.asInstanceOf[MemorySurfaceFormStore].size

    val cands = candMapStore.asInstanceOf[MemoryCandidateMapStore].candidates

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

    /*
    var c = 0
    var start = System.currentTimeMillis()
    val occSource1 = AllOccurrenceSource.fromXMLDumpFile(wikidump,Language.English)
    EntityTopicModelDocumentsSource.fromOccurrenceSource(occSource1,
        spotter,tokenizer,
        resStore,sfStore,candMapStore.asInstanceOf[MemoryCandidateMapStore].candidates,
        wikipediaToDBpediaClosure).foreach(doc => {

        c+=1

        if(c >= 1000) {
            println(System.currentTimeMillis() - start)
            start = System.currentTimeMillis()
            c -= 1000
        }
    }) */

    /*
    val store = StorehausCountStore.cachedOrFromConfig(storeName,storeHost,storePort)
    var start = System.currentTimeMillis()
    var c = 0

    var list = List[EntityTopicDocument]()
    val occSource = AllOccurrenceSource.fromXMLDumpFile(wikidump,Language.English)
    EntityTopicModelDocumentsSource.fromOccurrenceSource(occSource,spotter,tokenizer,resStore,sfStore,wikipediaToDBpediaClosure).foreach(doc => {
        list ::= doc
        c+= 1
        if(c % 1000 == 0) {
            println(System.currentTimeMillis() - start)
            start = System.currentTimeMillis()
            try {
                list.foreach(doc => gibbsSampleDocument(doc, store, numTopics, candMapStore, mentionSize, vocabSize, alpha, beta, gamma, delta, false))
            }
            catch {
                case ex:Throwable => ex.printStackTrace()
            }
            list = List[EntityTopicDocument]()
            println(System.currentTimeMillis() - start)
            start = System.currentTimeMillis()
            println()
        }


    })*/

    CountStore.cachedOrFromConfig(storeName, storeConf, typ = RedisCountStore).clear()

    val sparkHome = System.getenv("SPARK_HOME")

    val conf = new SparkConf()
      .setMaster(sparkCluster)
      .setAppName("Entity Topic Model Training")
      .set("spark.executor.memory", "7g")
      .set("spark.default.parallelism", parallelism.toString)
      .setSparkHome({
      if (sparkHome == null) "" else sparkHome
    })
      .setJars(StreamingContext.jarOfClass(this.getClass))
    //.setJars(Array("entity_topic/target/entity_topic-0.7.jar"))
    //conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    //conf.set("spark.kryo.registrator", "org.dbpedia.spotlight.train.EntityTopicKryoRegistrator")
    conf.set("spark.streaming.unpersist", "true")
    conf.set("spark.broadcast.blockSize", "10240")

    val sc = new SparkContext(conf)
    val candidates = sc.broadcast(cands)
    val counter = sc.accumulator(0)

    def setup(init: Boolean, rddQueue: mutable.SynchronizedQueue[RDD[EntityTopicDocument]], ssc: StreamingContext, dir: File) {
      ssc.queueStream(rddQueue, oneAtATime = false, defaultRDD = null).repartition(parallelism).mapPartitions(docs => {
        val store = CountStore.cachedOrFromConfig(storeName, storeConf, typ = RedisCountStore)
        val totalUpdates = mutable.Map[String, Int]()
        var ct = 0
        docs.foreach(doc => {
          val updates = SamplingFromCountStore.gibbsSampleDocument(doc, store, numTopics, candidates.value, mentionSize, vocabSize, alpha, beta, gamma, delta, init)
          updates.foreach(update => {
            totalUpdates += update._1 -> (totalUpdates.getOrElse(update._1, 0) + update._2)
          })
          ct += 1
          doc
        })
        store.multiMerge(totalUpdates)
        counter.add(ct)
        docs
      }).foreachRDD(rdd => {
        rdd.saveAsObjectFile(new File(dir, "ETD_" + System.currentTimeMillis()).getAbsolutePath)
      })
    }


    val ssc = new StreamingContext(sc, Seconds(1))
    val rddQueue = new mutable.SynchronizedQueue[RDD[EntityTopicDocument]]()

    var currentDir = new File(dataDir, "initialized")

    setup(init = true, rddQueue, ssc, currentDir)
    ssc.start()

    //Start streaming
    var ctr = 0
    var l = List[EntityTopicDocument]()
    val occSource = AllOccurrenceSource.fromXMLDumpFile(wikidump, Language.English)
    EntityTopicModelDocumentsSource.fromOccurrenceSource(occSource,
      spotter, tokenizer,
      resStore, sfStore, candMapStore.asInstanceOf[MemoryCandidateMapStore].candidates,
      wikipediaToDBpediaClosure).foreach(doc => {
      l ::= doc
      ctr += 1
      if (ctr % nrOfDocsInRDD == 0) {
        val rdd = ssc.sparkContext.makeRDD(l)
        rddQueue += rdd
        l = List[EntityTopicDocument]()
        LOG.info(s"$ctr entity topic documents pushed")
        LOG.info(s"${counter.value} entity topic documents sampled")
      }
    })


    //Ugly hack, wait until finished
    while (ctr > counter.value) {
      LOG.info(s"$ctr entity-topic-documents pushed")
      LOG.info(s"${counter.value} entity topic documents sampled")
      Thread.sleep(10000)
    }
    ssc.stop(false)

    //Training
    (0 until 300).foreach(i => {
      val ssc = new StreamingContext(sc, Seconds(1))
      val rddQueue = new mutable.SynchronizedQueue[RDD[EntityTopicDocument]]()
      val newDir = new File(dataDir, s"iteration$i")

      counter.value = 0

      setup(init = false, rddQueue, ssc, newDir)
      ssc.start()

      currentDir.listFiles().foreach(file => {
        val rdd = ssc.sparkContext.objectFile[EntityTopicDocument](file.getAbsolutePath)
        rddQueue += rdd

        LOG.info(s"${counter.value} of $ctr entity-topic-documents processed")

        file.deleteOnExit()
      })
      //Ugly hack, wait until finished
      while (ctr > counter.value) {
        LOG.info(s"$ctr entity-topic-documents pushed")
        LOG.info(s"${counter.value} entity topic documents processed")
        Thread.sleep(10000)
      }
      ssc.stop(false)
      currentDir.delete()
      currentDir = newDir
    })
    sc.stop()

    val store = CountStore.cachedOrFromConfig(storeName, storeConf)
    if (store.isInstanceOf[RedisCountStore])
      store.asInstanceOf[RedisCountStore].writerPool.withClient(_.save)

    System.exit(0)
  }

}

class EntityTopicKryoRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    kryo.register(classOf[EntityTopicDocument])
    kryo.register(classOf[Array[EntityTopicDocument]])
    kryo.register(classOf[Array[(String, Int)]])
    kryo.register(classOf[(String, Int)])
  }
}