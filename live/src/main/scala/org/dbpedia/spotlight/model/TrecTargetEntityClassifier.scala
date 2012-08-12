package org.dbpedia.spotlight.model

import java.io.File
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.topic.util.TopicUtil
import org.dbpedia.spotlight.topic.WekaMultiLabelClassifier
import collection.mutable._

/**
 * This is a wrapper class for for a topical multilabel classifier, which serves as a classifier is able to calculate
 * the relevance of a target-entity to a set of DBpediaResources.
 *
 * @param modelDir Place to save the whole model
 * @param targetEntities Set of DbpediaResources, that are considered as possible labels/classes.
 */
class TrecTargetEntityClassifier(val modelDir: File, var targetEntities: Set[DBpediaResource] = null) {

    private val LOG = LogFactory.getLog(getClass)

    private val topicsInfo = TopicUtil.getTopicInfo(modelDir.getAbsolutePath + "/topics.info")
    private val dictionary = TopicUtil.getDictionary(modelDir.getAbsolutePath + "/word_id.dict", 100000)

    if (topicsInfo.getTopics.size > 0)
        targetEntities = topicsInfo.getTopics.foldLeft(Set[DBpediaResource]())((set, topic) => set + (new DBpediaResource(topic.getName)))
    else {
        try {
            targetEntities.foreach(entity => {
                topicsInfo.newTopic(new Topic(entity.uri))
            })
        }
        catch {
            case e: NullPointerException => throw new NullPointerException("Target entities have to be specified if a new model is created")
        }
    }

    private val classifier =
        new WekaMultiLabelClassifier(dictionary,
            topicsInfo,
            new File(modelDir.getAbsolutePath + "/multilabel-model"), false)

    def update(targets: Set[DBpediaResource], annotations: Map[DBpediaResource, Double]) {
        val vector = vectorize(annotations)

        targets.foreach(target => {
            classifier.update(vector, new Topic(target.uri))
        })
    }


    private def vectorize(annotations: Map[DBpediaResource, Double]): Map[Int, Double] = {
        var vector = Map[Int, Double]()
        annotations.foreach {
            case (resource, count) => {
                val id = dictionary.getOrElsePut(resource.uri)
                if (id >= 0) {
                    vector += (id -> count)
                }
            }
        }

        val squaredSum = math.sqrt(vector.values.foldLeft(0.0)(_ + math.pow(_, 2)))
        vector.transform((id, count) => count / squaredSum)
    }

    def getPredictions(annotations: Map[DBpediaResource, Double]): Map[DBpediaResource, Double] = {
        val vector = vectorize(annotations)

        val predictions = classifier.getPredictions(vector.keysIterator.toArray, vector.valuesIterator.toArray)
        predictions.foldLeft(Map[DBpediaResource, Double]())((map, topic) => map + (new DBpediaResource(topic._1.getName) -> topic._2))
    }

    def persist {
        classifier.persist
    }
}
