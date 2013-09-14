package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{Topic, DBpediaResource}
import io.Source
import java.io.{IOException, File}
import com.officedepot.cdap2.collection.CompactHashMap
import org.dbpedia.spotlight.log.SpotlightLog

/**
 *
 * @author pablomendes
 */

trait TopicalPriorStore {

  //    def getTopicalPriorCounts(resource:DBpediaResource): Map[Topic,Int]
  //    def getTopicalPriorCount(resource:DBpediaResource, topic: Topic): Int
  //    def getTotalCounts(): Map[Topic,Int]

  def getTopicalPriorCounts(resource:DBpediaResource): Map[Topic,Int]
  def getTopicalPriorCount(resource:DBpediaResource, topic: Topic): Int
  def getTotalCounts(): Map[Topic,Int]

}

object HashMapTopicalPriorStore extends TopicalPriorStore {
  val totalCounts = new CompactHashMap[Topic,Int]()                          // topic -> total
  val topicalPriors = new CompactHashMap[Topic,CompactHashMap[DBpediaResource,Int]]() // topic -> (resource -> count)

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
  def getTopicalPriorCounts(resource:DBpediaResource): Map[Topic,Int] = {
    topicalPriors.keys.map( topic => {
      topic -> getTopicalPriorCount(resource,topic)
    }).toMap
  }
  def getTopicalPriorCount(resource: DBpediaResource, topic: Topic): Int =  {
    val statsForTopic = topicalPriors.getOrElse(topic, new CompactHashMap[DBpediaResource,Int])
    statsForTopic.getOrElse(resource, 0)
  }

  def fromDir(dir: File) : TopicalPriorStore = {
    SpotlightLog.info(this.getClass, "Loading topical priors.")
    if (dir.exists() && dir.isDirectory) {
      dir.listFiles().foreach( file => {
        if (file.getName.endsWith(".count")) {
          var total = 0
          val topic = new Topic(file.getName.replaceAll(".count","").trim)
          val statsForTopic = new CompactHashMap[DBpediaResource,Int]
          Source.fromFile(file).getLines()
            //.take(5)
            .foreach( line => {
            line match {
              case ExtractCountAndResource(count,uri) => {
                val c = count.toInt
                statsForTopic.put(new DBpediaResource(uri.trim),c)
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
    SpotlightLog.info(this.getClass, "Done.")
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
      println("topic: %s; resources: %d".format(topic,topicalPriors.getOrElse(topic, new CompactHashMap[DBpediaResource,Int]).size))
    })

    println("# topics:  "+topicalPriors.keys.size)

  }

  def test() {
    //        val mjCount = getTopicalPriorCount(new DBpediaResource("Michael_Jackson"), new Topic(TopicUtil.CATCH_TOPIC))
    //        val otherCount = getTotalCounts().getOrElse(new Topic(TopicUtil.CATCH_TOPIC),0)
    //        println("MJ distribution: "+getTopicalPriorCounts(new DBpediaResource("Michael_Jackson")))
    //        println("c(MJ,other): "+mjCount)
    //        println("c(other): "+otherCount)
    //        println("p(MJ|other): "+ mjCount.toDouble / otherCount)
    //        println("log(p(MJ|other)): "+ scala.math.log(mjCount.toDouble / otherCount))
  }

}
