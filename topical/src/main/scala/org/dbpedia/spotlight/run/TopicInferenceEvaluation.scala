package org.dbpedia.spotlight.run

import scala.io.Source
import collection.mutable._
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.topical.util.TopicInferrer
import org.dbpedia.spotlight.db.model.HashMapTopicalPriorStore
import org.dbpedia.spotlight.topical.{FeedListener, Feed}
import org.dbpedia.spotlight.topical.trec.TrecResourceAnnotationFeed

/**
 * Little evaluation for topical inference from annotations.
 */
object TopicInferenceEvaluation {

    def main(args: Array[String]) {
        val corpusStream = getClass.getClassLoader.getResourceAsStream("tiny.corpus.tsv")

        var configuration: SpotlightConfiguration = null
        //Initialization, check values
        try {
            val configFileName: String = args(0)
            configuration = new SpotlightConfiguration(configFileName)
        }
        catch {
            case e: Exception => {
                e.printStackTrace
                System.exit(1)
            }
        }

        val factory = new SpotlightFactory(configuration)
        val trecTopicInferrer = new TopicInferrer(HashMapTopicalPriorStore)
        val topicsQueue = new Queue[Topic]()

        var meanSqErrorAn = 0.0
        var meanSqErrorAnAndTar = 0.0
        var ctr = 0

        var errorByTopicAn = Map[Topic, Double]()
        var errorByTopicAnAndTar = Map[Topic, Double]()
        var done = false

        var begin: Long = 0
        var end: Long = 0

        val corpusFeed = new Feed[(Set[DBpediaResource], Text)](true) {
            def act {
                println("Starting Evaluation!")
                begin = System.currentTimeMillis()

                Source.fromInputStream(corpusStream).getLines().foreach(line => {
                    val split = line.split("\t", 3)
                    topicsQueue.enqueue(new Topic(split(0)))

                    notifyListeners((Set(new DBpediaResource(split(1))), new Text(split(2))))
                })

                end = System.currentTimeMillis()
                done = true
            }

        }

        val annotationFeed = new TrecResourceAnnotationFeed(factory.annotator(), corpusFeed)

        var done2 = false
        val listener = new FeedListener[(Set[DBpediaResource], Text, Map[DBpediaResource, Double])]() {
            def update(item: (Set[DBpediaResource], Text, Map[DBpediaResource, Double])) {
                val topicsFromAn = trecTopicInferrer.inferTopics(item._3)
                val topicsFromAnAndTar = trecTopicInferrer.inferTopics(item._3, item._1)

                val topic = topicsQueue.dequeue()

                meanSqErrorAn += topicsFromAn.foldLeft(0.0)((sum, prediction) => {
                    val value = math.pow({
                        if (prediction._1.equals(topic)) 1.0 else 0.0
                    } - prediction._2, 2)
                    errorByTopicAn.update(prediction._1, errorByTopicAn.getOrElseUpdate(prediction._1, 0.0) + value)
                    sum + value
                })

                meanSqErrorAnAndTar += topicsFromAnAndTar.foldLeft(0.0)((sum, prediction) => {
                    val value = math.pow({
                        if (prediction._1.equals(topic)) 1.0 else 0.0
                    } - prediction._2, 2)
                    errorByTopicAnAndTar.update(prediction._1, errorByTopicAnAndTar.getOrElseUpdate(prediction._1, 0.0) + value)
                    sum + value
                })

                ctr += 1
                if (ctr % 10 == 0) {
                    println(ctr + " examples processed")
                }

                if (done)
                    done2 = true

            }

        }

        listener.subscribeTo(annotationFeed.textAnnotationFeed)
        annotationFeed.startFeed
        corpusFeed.start()

        while (!done2)
            Thread.sleep(10000)


        println("==== Just Annotations ====")
        println("Mean Squared Error: " + meanSqErrorAn / ctr)
        print(errorByTopicAn.foldLeft("")((string, mse) => string + mse._1.getName + ": " + mse._2 / ctr + "\n"))
        println()
        println("==== Annotations and Targets ====")
        println("Mean Squared Error: " + meanSqErrorAnAndTar / ctr)
        print(errorByTopicAnAndTar.foldLeft("")((string, mse) => string + mse._1.getName + ": " + mse._2 / ctr + "\n"))
        println()
        println("Total number of examples: " + ctr)
        val time = ((end - begin) / 1000)
        println("Average processing time: " + ctr.toDouble / time + " examples/sec")
        println("Total time elapsed: " + time + " sec")

    }


}
