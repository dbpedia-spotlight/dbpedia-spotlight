package org.dbpedia.spotlight.train

import org.apache.spark.{SparkConf, SparkContext}
import scala.collection.mutable
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.dbpedia.spotlight.io.{EntityTopicDocument, AllOccurrenceSource, EntityTopicModelDocumentsSource}
import org.dbpedia.extraction.util.Language
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.db.{FSASpotter, SpotlightModel}
import org.dbpedia.spotlight.db.memory.{MemorySurfaceFormStore, MemoryCandidateMapStore, MemoryStore}
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.db.tokenize.LanguageIndependentTokenizer
import java.util.Locale
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import org.dbpedia.spotlight.storage.StorehausCountStore.StorehausCountStore
import scala.Array
import com.twitter.util.Await
import org.dbpedia.spotlight.storage.StorehausCountStore
import org.dbpedia.spotlight.storage.StorehausCountStore.StorehausCountStore
import org.dbpedia.spotlight.db.model.CandidateMapStore
import scala.util.Random
import org.apache.spark.storage.StorageLevel


/**
 * Created by dirkw on 3/11/14.
 */
object TrainEntityTopicModelWithSpark {

    def main(args:Array[String]) {
        val wikidump = new File(args(0))
        val modelDir = new File(args(1))
        val sparkCluster = args(2)
        val storeHost = args(3)
        val storePort = args(4).toInt
        val numTopics = args(5).toInt
        val dataDir = new File(args(6))

        val storeName = "entity_topic_model_store"

        val (tokenStore,sfStore,resStore,candMapStore,_) = SpotlightModel.storesFromFolder(modelDir)
        val vocabSize = tokenStore.getVocabularySize
        val mentionSize = sfStore.asInstanceOf[MemorySurfaceFormStore].size

        val beta = 0.1
        val gamma = 0.0001
        val delta =  2000.0 / vocabSize
        val alpha = 50.0/numTopics

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
            new LanguageIndependentTokenizer(stopwords, new SnowballStemmer("EnglishStemmer"), new Locale("en", "US"), tokenStore)

        val conf = new SparkConf()
            .setMaster(sparkCluster)
            .setAppName("Entity Topic Model Training")
            .set("spark.executor.memory", "1g")
            .setSparkHome(System.getenv("SPARK_HOME"))
            .setJars(StreamingContext.jarOfClass(this.getClass))

        val sc = new SparkContext(conf)

        def setup(init:Boolean, rddQueue: mutable.SynchronizedQueue[RDD[EntityTopicDocument]], ssc:StreamingContext, dir:File) {
            val docs = ssc.queueStream(rddQueue)
            val stream = docs.map(doc => {
                val store = StorehausCountStore.cachedOrFromConfig(storeName,storeHost,storePort)
                val updates = gibbsSampleDocument(doc, store, numTopics, candMapStore, mentionSize, vocabSize, alpha, beta, gamma, delta, init)
                store.multiMerge(updates)
                doc
            })

            stream.saveAsObjectFiles(new File(dir,"entity_topic_document").getAbsolutePath)
        }
        
        
        val ssc = new StreamingContext(sc, Seconds(1))
        val rddQueue = new mutable.SynchronizedQueue[RDD[EntityTopicDocument]]()

        var currentDir = new File(dataDir,"initialized")

        setup(init = true, rddQueue, ssc, currentDir)
        ssc.start()
        //Start streaming
        var ctr = 0
        var l = List[EntityTopicDocument]()
        val occSource = AllOccurrenceSource.fromXMLDumpFile(wikidump,Language.English)
        EntityTopicModelDocumentsSource.fromOccurrenceSource(occSource,spotter,tokenizer,resStore).foreach(doc => {
            l ::= doc
            if(ctr == 1000) {
                rddQueue += ssc.sparkContext.makeRDD(l)
                ctr = 0
                l = List[EntityTopicDocument]()
            }
        })

        Thread.sleep(60000)
        ssc.stop(stopSparkContext = false)

        //Training
        (0 until 300).foreach(i => {
            val ssc = new StreamingContext(sc, Seconds(1))
            val rddQueue = new mutable.SynchronizedQueue[RDD[EntityTopicDocument]]()
            val newDir = new File(dataDir,s"iteration$i")
            setup(init = false, rddQueue, ssc,newDir)

            ssc.start()

            currentDir.listFiles().foreach(file => {
                rddQueue += ssc.sparkContext.objectFile[EntityTopicDocument](file.getAbsolutePath)
                file.deleteOnExit()
            })
            Thread.sleep(60000)
            ssc.stop(false)
            currentDir.delete()
            currentDir = newDir
        })
        sc.stop()
    }


    private def gibbsSampleDocument(doc: EntityTopicDocument, store: StorehausCountStore,
                                    numTopics: Int, candMapStore: CandidateMapStore, mentionSize: Int, vocabSize: Int,
                                    alpha: Double, beta: Double, gamma: Double, delta: Double,
                                    init:Boolean = false) = {
        var updates = Map[String, Int]()
        def incr(key: String, value: Int) = updates += key -> (updates.getOrElse(key, 0) + value)

        {
            //Sample new topics
            val (topicCounts, topicEntityCounts) = getTopicCountsForDoc(doc, numTopics, store)
            val docTopicCounts = doc.entityTopics.foldLeft(Map[Int, Int]())((acc, topic) => {
                if (topic >= 0)
                    acc + (topic -> (acc.getOrElse(topic, 0) + 1))
                else
                    acc
            })
            (0 until doc.entityTopics.length).foreach(idx => {
                val entity = doc.mentionEntities(idx)
                val oldTopic = doc.entityTopics(idx)

                val newTopic = sampleFromProportionals(topic => {
                    val add = {
                        if (topic == oldTopic) -1 else 0
                    }
                    (docTopicCounts.getOrElse(topic, 0) + add + alpha) *
                        (topicEntityCounts(entity)(topic) + add + beta) / (topicCounts(topic) + add + numTopics * beta)
                }, 0 until numTopics)

                doc.entityTopics(idx) = newTopic

                if (!init && oldTopic >= 0) {
                    incr(getCTEKey(oldTopic, entity), -1)
                    incr(getCTKey(oldTopic), -1)
                }
                incr(getCTEKey(newTopic, entity), 1)
                incr(getCTKey(newTopic), 1)
            })
        }

        {
            //Sample new entities
            val entityCounts = getEntityCountsForDoc(doc, candMapStore.asInstanceOf[MemoryCandidateMapStore], store)
            val docEntityCounts = doc.mentionEntities.foldLeft(Map[Int, Int]())((acc, entity) => {
                if (entity >= 0)
                    acc + (entity -> (acc.getOrElse(entity, 0) + 1))
                else
                    acc
            })
            val docAssignmentCounts = doc.tokenEntities.foldLeft(Map[Int, Int]())((acc, entity) => {
                if (entity >= 0)
                    acc + (entity -> (acc.getOrElse(entity, 0) + 1))
                else
                    acc
            })
            (0 until doc.mentionEntities.length).foreach(idx => {
                val oldEntity = doc.mentionEntities(idx)
                val mention = doc.mentions(idx)
                val newEntity = sampleFromProportionals(entity => {
                    val add = {
                        if (entity == oldEntity) -1 else 0
                    }
                    val (cte, cem, ce) = entityCounts(idx)(entity)
                    (cte + add + beta) *
                        (cem + add + gamma) / (ce + add + mentionSize * gamma) *
                        (0 until docAssignmentCounts(entity)).foldLeft(1)((acc, _) => acc * (docEntityCounts(entity) + 1) / docEntityCounts(entity))
                }, entityCounts(idx).keySet)

                doc.mentionEntities(idx) = newEntity

                if (!init && oldEntity >= 0) {
                    incr(getCEMKey(oldEntity, mention), -1)
                    incr(getCEOfMKey(oldEntity), -1)
                }
                incr(getCEMKey(newEntity, mention), 1)
                incr(getCEOfMKey(newEntity), 1)
            })
        }

        {
            //Sample new assignments
            val (wordEntityCounts, entityCountsForWords) = getAssignmentCountsForDoc(doc, store)
            val docAssignmentCounts = doc.tokenEntities.foldLeft(Map[Int, Int]())((acc, entity) => {
                if (entity >= 0)
                    acc + (entity -> (acc.getOrElse(entity, 0) + 1))
                else
                    acc
            })
            val candidateEntities = doc.mentionEntities.toSet
            (0 until doc.tokenEntities.length).foreach(idx => {
                val oldEntity = doc.tokenEntities(idx)
                val token = doc.tokens(idx)
                val newEntity = sampleFromProportionals(entity => {
                    val add = {
                        if (entity == oldEntity) -1 else 0
                    }
                    docAssignmentCounts(entity) *
                        (wordEntityCounts(idx)(entity) + add + delta) / (entityCountsForWords(entity) + add + vocabSize * delta)
                }, candidateEntities)

                doc.tokenEntities(idx) = newEntity

                if (!init && oldEntity >= 0) {
                    incr(getCEWKey(oldEntity, token), -1)
                    incr(getCEOfWKey(oldEntity), -1)
                }
                incr(getCEWKey(newEntity, token), 1)
                incr(getCEOfWKey(newEntity), 1)
            })
        }

        updates.filterNot(_._2 == 0)
    }

    def sampleFromProportionals(calcProportion:Int => Double, candidates:Traversable[Int]) = {
        var sum = 0.0
        val cands = candidates.map(cand => {
            var res = calcProportion(cand)
            sum += res
            (cand, res)
        })
        var random = Random.nextDouble() * sum
        var selected = (-1, 0.0)
        val it = cands.toIterator
        while (random >= 0.0) {
            selected = it.next()
            random -= selected._2
        }
        selected._1
    }

    private val cte_prefix = "cte_"
    private val cem_prefix = "cem_"
    private val cew_prefix = "cew_"

    private def getCTEKey(topic:Int,entity:Int) = cte_prefix+topic+"-"+entity
    private def getCTKey(topic:Int) = cte_prefix+topic

    private def getCEMKey(entity:Int,mention:Int) = cem_prefix+entity+"-"+mention
    private def getCEOfMKey(entity:Int) = cem_prefix+entity

    private def getCEWKey(entity:Int,word:Int) = cew_prefix+entity+"-"+word
    private def getCEOfWKey(entity:Int) = cew_prefix+entity

    //ct* -> total counts of topics; cte -> array over all topics for each entity
    def getTopicCountsForDoc(doc:EntityTopicDocument, numTopics:Int, store:StorehausCountStore) = {
        val keys = (0 until numTopics).flatMap(topic => {
            Set(getCTKey(topic)) ++ doc.entityTopics.flatMap(entity  => {
                if(entity  >= 0)
                    Some(getCTEKey(topic,entity))
                else
                    None
            })
        }).toSet

        val result = store.multiGet(keys)

        val topicCounts = (0 until numTopics).map(topic => {
            Await.result{ result(getCTKey(topic)) }.getOrElse(0)
        }).toArray

        val entityTopicCounts = doc.mentionEntities.map(entity => {
            (0 until numTopics).map(topic => {
                if(entity  < 0)
                    0
                else {
                    Await.result{ result(getCTEKey(topic,entity)) }.getOrElse(0)
                }
            }).toArray
        }).toArray

        (topicCounts,entityTopicCounts)
    }

    //cte; cem; ce* for all entities for each mention
    def getEntityCountsForDoc(doc:EntityTopicDocument, candMapStore:MemoryCandidateMapStore, store:StorehausCountStore) = {
        val keys = (0 until doc.mentions.length).flatMap(idx => {
            val mention = doc.mentions(idx)
            val topic = doc.entityTopics(idx)
            candMapStore.candidates(mention).flatMap(entity => {
                Set(getCEMKey(entity,mention), getCEOfMKey(entity), getCTEKey(topic,entity))
            })
        }).toSet

        val result = store.multiGet(keys)

        (0 until doc.mentions.length).map(idx => {
            val mention = doc.mentions(idx)
            val topic = doc.entityTopics(idx)
            candMapStore.candidates(mention).map(entity => {
                entity -> {
                    val cem = Await.result { result(getCEMKey(entity,mention)) }.getOrElse(0)
                    val ce = Await.result { result(getCEOfMKey(entity)) }.getOrElse(0)
                    val cte = Await.result { result(getCTEKey(topic,entity)) }.getOrElse(0)
                    (cte,cem,ce)
                }
            }).toMap
        }).toArray
    }

    def getAssignmentCountsForDoc(doc:EntityTopicDocument, store:StorehausCountStore) = {
        val keys = doc.tokens.flatMap(word => {
            doc.mentionEntities.flatMap(entity => {
                Set(getCEWKey(entity,word), getCEOfWKey(entity))
            })
        }).toSet

        val result = store.multiGet(keys)

        val candidates = doc.mentionEntities.toSet
        val wordEntities = doc.tokens.map(word => {
            candidates.map(entity =>
                entity -> { Await.result{ result(getCEWKey(entity,word)) }.getOrElse(0) } ).toMap
        })

        val entityCounts = candidates.map(entity =>
            entity -> { Await.result{ result(getCEOfWKey(entity)) }.getOrElse(0) } ).toMap

        (wordEntities,entityCounts)
    }

}
