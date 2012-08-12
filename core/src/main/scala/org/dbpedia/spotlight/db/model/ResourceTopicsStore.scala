package org.dbpedia.spotlight.db.model

import java.io.{FileNotFoundException, File}
import org.dbpedia.spotlight.model.{Topic, DBpediaResource}
import com.officedepot.cdap2.collection.CompactHashMap
import io.Source
import org.apache.commons.logging.LogFactory

/**
 * this class loads topical vectors for resources into memory which where created by CalculateResourceTopicVectors
 *
 * @author dirk
 */
trait ResourceTopicsStore {
    def pTopicGivenResource(resource: DBpediaResource, topic: Topic) : Double
    def pTopicGivenResource(resource: DBpediaResource) : Map[Topic,Double]

}

object HashMapResourceTopicStore extends ResourceTopicsStore {
    private val LOG = LogFactory.getLog(this.getClass)

    private val topicGivenResource = new CompactHashMap[DBpediaResource,CompactHashMap[Topic,Double]]()

    def fromFile(file:File):ResourceTopicsStore = {
        LOG.info("Loading probabilities for topics given resources.")
        if (file.exists() && file.isFile) {
            Source.fromFile(file).getLines().foreach(line => {
                val split = line.split(" ",2)
                val resource = new DBpediaResource(split(0))
                val topics = new CompactHashMap[Topic,Double]()

                split(1).split(" ").foreach(topicString => {
                    val topic_prediction = topicString.split(":")

                    topics.put(new Topic(topic_prediction(0)), topic_prediction(1).toDouble)
                })
                topicGivenResource.put(resource, topics)
            })
        }
        else
            throw new FileNotFoundException("resource-topics file not found")

        this
    }

    def pTopicGivenResource(resource: DBpediaResource, topic: Topic) : Double = {
        topicGivenResource.getOrElse(resource,Map[Topic,Double]()).getOrElse(topic,-1)
    }

    def pTopicGivenResource(resource: DBpediaResource) : Map[Topic,Double] = {
        topicGivenResource.getOrElse(resource,Map[Topic,Double]()).toMap
    }

}
