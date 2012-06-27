package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{Topic, DBpediaResource}
import io.Source
import java.io.{IOException, File}
import com.officedepot.cdap2.collection.CompactHashMap
import org.apache.commons.logging.LogFactory

/**
 *
 * @author pablomendes
 */

trait TopicalPriorStore {

//    def getTopicalPriorCounts(resource:DBpediaResource): Map[Topic,Int]
//    def getTopicalPriorCount(resource:DBpediaResource, topic: Topic): Int
//    def getTotalCounts(): Map[Topic,Int]

    def getTopicalPriorCounts(resource:DBpediaResource): Map[String,Int]
    def getTopicalPriorCount(resource:DBpediaResource, topic: String): Int
    def getTotalCounts(): Map[String,Int]

}

object HashMapTopicalPriorStore extends TopicalPriorStore {
    private val LOG = LogFactory.getLog(this.getClass)

    val totalCounts = new CompactHashMap[String,Int]()                          // topic -> total
    val topicalPriors = new CompactHashMap[String,CompactHashMap[String,Int]]() // topic -> (resource -> count)

    val ExtractCountAndResource = """^\s+(\d+)\s+(\S+)$""".r

//    def getTotalCounts() = {
//        totalCounts.map{ case (topic,count) => new Topic(topic) -> count }.toMap
//    }
//
//    def getTopicalPriorCounts(resource:DBpediaResource): Map[Topic,Int] = {
//        topicalPriors.keys.map( topic => {
//            new Topic(topic) -> getTopicalPriorCount(resource.uri,topic)
//        }).toMap
//    }
//
//    def getTopicalPriorCount(resource: DBpediaResource, topic: Topic): Int = getTopicalPriorCount(resource.uri, topic.name)

    def getTotalCounts() = {
        totalCounts.toMap
    }
    def getTopicalPriorCounts(resource:DBpediaResource): Map[String,Int] = {
        topicalPriors.keys.map( topic => {
            topic -> getTopicalPriorCount(resource.uri,topic)
        }).toMap
    }
    def getTopicalPriorCount(resource: DBpediaResource, topic: String): Int = getTopicalPriorCount(resource.uri, topic)


    private def getTopicalPriorCount(uri: String, topic: String): Int = {
        val statsForTopic = topicalPriors.getOrElse(topic, new CompactHashMap[String,Int])
        statsForTopic.getOrElse(uri, 0)
    }

    def fromDir(dir: File) : TopicalPriorStore = {
        LOG.info("Loading topical priors.")
        if (dir.exists() && dir.isDirectory) {
                    dir.listFiles().foreach( file => {
                        if (file.getName.endsWith(".count")) {
                            var total = 0
                            val topic = file.getName.replaceAll(".count","").trim
                            val statsForTopic = new CompactHashMap[String,Int]
                            Source.fromFile(file).getLines()
                                //.take(5)
                                .foreach( line => {
                                    line match {
                                        case ExtractCountAndResource(count,uri) => {
                                            val c = count.toInt
                                            statsForTopic.put(uri.trim,c)
                                            total = total + c
                                        }
                                        case _ => println("no match")
                                    }
                            })
                            topicalPriors.put(topic,statsForTopic)
                            totalCounts.put(topic, total)
                        }
                    })
                } else {
                    throw new IOException("Could not load directory with topics.")
                }
        LOG.info("Done.")
        this
    }

    def main(args: Array[String]) {
        val dir = if (args.size>0) new File(args(0)) else new File("data/topics")
        fromDir(dir)
        stats()
        test()
    }

    def stats() {

        println("topics "+topicalPriors.keys.size)

        topicalPriors.keys.foreach( topic => {
            println("topic: %s; resources: %d".format(topic,topicalPriors.getOrElse(topic, new CompactHashMap[String,Int]).size))
        })

        println("# topics:  "+topicalPriors.keys.size)

    }

    def test() {
//        val mjCount = getTopicalPriorCount(new DBpediaResource("Michael_Jackson"), new Topic("other"))
//        val otherCount = getTotalCounts().getOrElse(new Topic("other"),0)
//        println("MJ distribution: "+getTopicalPriorCounts(new DBpediaResource("Michael_Jackson")))
//        println("c(MJ,other): "+mjCount)
//        println("c(other): "+otherCount)
//        println("p(MJ|other): "+ mjCount.toDouble / otherCount)
//        println("log(p(MJ|other)): "+ scala.math.log(mjCount.toDouble / otherCount))
    }

}
