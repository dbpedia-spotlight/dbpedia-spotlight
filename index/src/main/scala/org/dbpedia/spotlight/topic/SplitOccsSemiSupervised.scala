package org.dbpedia.spotlight.topic

import scala.collection.mutable.Map
import scala.collection.mutable.Set
import java.io.{FileWriter, PrintWriter, File}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.io.FileOccurrenceSource
import org.dbpedia.spotlight.model.{TopicDescription, DBpediaResource, Topic}
import org.dbpedia.spotlight.util.{TextVectorizer, IndexingConfiguration}
import org.dbpedia.spotlight.db.model.WordIdDictionary
import org.dbpedia.spotlight.topic.util.TopicUtil
import org.dbpedia.spotlight.topic.convert.TextCorpusToInputCorpus
import wikipedia.util.WikipediaHierarchyLoader

/**
 * This object splits the occs file into several topical occs files, by first creating an initial split, which is done
 * by a keyword matching approach, i.e. it compares a list of keywords defined in the topic.description file (which location is specified
 * in the properties) to the name of the resource of an occ. The initial split is than used to train an initial topical
 * classifier which is then used to assign the rest of the unassigned occs to the topics.
 *
 * @author dirk
 */
//TODO just allow concept uris
object SplitOccsSemiSupervised {
    private val LOG = LogFactory.getLog(getClass)

    /**
     *
     * @param args 1st: indexing.properties 2nd: path to (sorted) occs file, 3rd: temporary path (same partition as output)
     *             , 4th: minimal confidence of assigning an occ to a topic, 5th: nr of iterations, 6th: path to output directory
     *
     */
    def main(args: Array[String]) {
        if (args.length > 5) {
            val config = new IndexingConfiguration(args(0))
            splitOccs(args(1), config.get("org.dbpedia.spotlight.topic.description"), args(2), args(3).toDouble, args(4).toInt, args(5))
        }
        else
            LOG.error("Not sufficient arguments!")
    }

    def splitOccs(pathToOccsFile: String, pathToTopicDescription: String, tmpPath: String, threshold: Double, iterations: Int, outputPath: String) {
        val tmpCorpus = tmpPath + "/tmpCorpus.tsv"
        val tmpTopics = tmpPath + "/tmpTopics.info"
        val tmpDic = tmpPath + "/tmpDic.dic"
        val tmpArff = tmpPath + "/tmpArff.arff"
        val tmpModel = tmpPath + "/tmpModel"
        val tmpOtherPath = tmpPath + "/tmpOther.tsv"

        initialSplit(pathToTopicDescription, pathToOccsFile, tmpOtherPath, outputPath)

        for (i <- 0 until iterations) {
            GenerateOccTopicCorpus.generateCorpus(outputPath, -1, tmpCorpus + ".tmp")
            new ProcessBuilder("sort", "-R", "-o", tmpCorpus, tmpCorpus + ".tmp").start().waitFor()
            new File(tmpCorpus + ".tmp").delete()

            TextCorpusToInputCorpus.writeDocumentsToCorpus(tmpCorpus, tmpArff, false, true, "arff", tmpTopics, tmpDic, 100000, 100000)
            WekaMultiLabelClassifier.trainModel(tmpArff, tmpModel)
            val dictionary = new WordIdDictionary(tmpDic)
            val classifier = new WekaSingleLabelClassifier(dictionary, TopicUtil.getTopicInfo(tmpTopics), new File(tmpModel))

            AssignTopicsToOccs.assignTopics(tmpOtherPath, classifier, threshold, outputPath, true)
            val tmpOtherFile = new File(tmpOtherPath)
            tmpOtherFile.delete()
            if (i < iterations - 1)
                new File(outputPath + "/other.tsv").renameTo(tmpOtherFile)
        }

        new File(tmpPath).delete()
    }

    def initialSplit(pathToTopicDescription: String,
                     pathToOccsFile: String,
                     pathToRestOccs: String,
                     outputPath: String) {
        def getScore(categoryName: Set[String], matchName: String): Double = {
            val parts = matchName.split("_")
            parts.foreach(part => if (!categoryName.contains(part)) return 0.0)

            return 1.0
        }

        new File(outputPath).mkdirs()
        val writers = Map[Topic, PrintWriter]()
        val topicDescriptions = TopicDescription.fromDescriptionFile(pathToTopicDescription)
        val vectorizer = new TextVectorizer()

        topicDescriptions.foreach(description => {
            writers += (description.topic -> new PrintWriter(new FileWriter(outputPath + "/" + description.topic.getName + ".tsv")))
        })
        val restWriter = new PrintWriter(new FileWriter(pathToRestOccs))
        var assignedResourcesCtr = 0
        var ctr = 0

        var lastResource: DBpediaResource = new DBpediaResource("")
        val selectedTopics = Map[Topic, Double]()
        FileOccurrenceSource.fromFile(new File(pathToOccsFile)).foreach(occ => {
            if (!occ.resource.equals(lastResource)) {
                selectedTopics.clear()
                lastResource = occ.resource

                val resourceNameAsSet = Set() ++ vectorizer.getWordCountVector(lastResource.uri.toLowerCase.replaceAll("[^a-z]", " ")).keySet
                topicDescriptions.foreach(topicInfo => {
                    var sum = 0.0
                    topicInfo.keywords.foreach(keyword => sum += getScore(resourceNameAsSet, keyword))

                    if (sum >= 1.0) {
                        selectedTopics += (topicInfo.topic -> sum)
                    }
                })
            }

            if (selectedTopics.size > 0) {
                var topics = selectedTopics.toList.sortBy(x => -x._2)
                topics = topics.takeWhile {
                    case (topic, value) => value > 0.9 * topics.head._2
                }
                topics.foreach(topic => {
                    writers(topic._1).println(occ.toTsvString)
                })
                assignedResourcesCtr += 1
                if (assignedResourcesCtr % 10000 == 0) {
                    LOG.info("Assigned " + assignedResourcesCtr + " resources to topics")
                    LOG.info("Latest assignment: " + lastResource.uri + " -> " + topics.head._1.getName)
                }
            }
            else
                restWriter.println(occ.toTsvString)

            ctr += 1
            if (ctr % 100000 == 0)
                LOG.info(ctr + " occs processed!")
        })

        writers.foreach(_._2.close())
        restWriter.close()
    }
}
