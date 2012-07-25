package org.dbpedia.spotlight.topic

import scala.collection.mutable.Map
import scala.collection.mutable.Set
import java.io.{FileWriter, PrintWriter, File}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.io.{FileOccurrenceSource}
import org.dbpedia.spotlight.model.{TopicInformation, DBpediaResource, Topic}
import org.dbpedia.spotlight.util.IndexingConfiguration
import org.dbpedia.spotlight.topic.utility.{TextVectorizer, TopicUtil, WordIdDictionary}
import org.dbpedia.spotlight.topic.convert.{TextCorpusToInputCorpus}

object SplitOccsSemiSupervised {
  private val LOG = LogFactory.getLog(getClass)

  /**
   *
   * @param args 1st: indexing.properties 2nd: path to (sorted) occs file, 3rd: temporary path (same partition as output)
   *             , 4th: minimal confidence of assigning an occ to a topic, 5th: iterations, 6th: path to output directory
   *
   */
  def main(args: Array[String]) {
    if (args.length > 5) {
      val config = new IndexingConfiguration(args(0))
      splitOccs(args(1), config.get("org.dbpedia.spotlight.topic.info"), args(2), args(3).toDouble, args(4).toInt, args(5))
    }
    else
      LOG.error("Not sufficient arguments!")
  }

  def splitOccs(pathToOccsFile: String, pathToTopicInfo: String, tmpPath: String, threshold: Double, iterations: Int, outputPath: String) {
    val tmpCorpus = tmpPath + "/tmpCorpus.tsv"
    val tmpTopics = tmpPath + "/tmpTopics.info"
    val tmpDic = tmpPath + "/tmpDic.dic"
    val tmpArff = tmpPath + "/tmpArff.arff"
    val tmpModel = tmpPath + "/tmpModel"
    val tmpOtherPath = tmpPath + "/tmpOther.tsv"

    initialSplit(pathToTopicInfo, pathToOccsFile, tmpOtherPath, outputPath)

    for (i <- 0 until iterations) {
      GenerateOccTopicCorpus.generateCorpus(outputPath, -1, tmpCorpus + ".tmp")
      new ProcessBuilder("sort", "-R", "-o", tmpCorpus, tmpCorpus + ".tmp").start().waitFor()
      new File(tmpCorpus + ".tmp").delete()

      TextCorpusToInputCorpus.writeDocumentsToCorpus(tmpCorpus, tmpArff, false, true, "arff", tmpTopics, tmpDic, 100000, 100000)
      WekaMultiLabelClassifier.trainModel(tmpArff, tmpModel)
      val dictionary = new WordIdDictionary(tmpDic)
      val classifier = new WekaMultiLabelClassifier(dictionary, TopicUtil.getTopicInfo(tmpTopics), new File(tmpModel))

      AssignTopicsToOccs.assignTopics(tmpOtherPath, classifier, threshold, outputPath, true)
      val tmpOtherFile = new File(tmpOtherPath)
      tmpOtherFile.delete()
      if (i < iterations-1)
        new File(outputPath + "/other.tsv").renameTo(tmpOtherFile)
    }

    new File(tmpPath).delete()
  }

  def initialSplit(pathToTopicInfo: String,
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
    val topicalInformation = TopicInformation.fromDescriptionFile(pathToTopicInfo)
    val vectorizer = new TextVectorizer()

    topicalInformation.foreach(topicInfo => {
      writers += (topicInfo.topic -> new PrintWriter(new FileWriter(outputPath + "/" + topicInfo.topic.getName + ".tsv")))
    })
    val otherWriter = new PrintWriter(new FileWriter(pathToRestOccs))
    var assignedResourcesCtr = 0
    var ctr = 0

    var lastResource: DBpediaResource = new DBpediaResource("")
    val selectedTopics = Map[Topic, Double]()
    FileOccurrenceSource.fromFile(new File(pathToOccsFile)).foreach(occ => {
      if (!occ.resource.equals(lastResource)) {
        selectedTopics.clear()
        lastResource = occ.resource

        val resourceNameAsSet = Set() ++ vectorizer.getWordCountVector(lastResource.uri.toLowerCase.replaceAll("[^a-z]", " ")).keySet
        topicalInformation.foreach( topicInfo => {
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
        otherWriter.println(occ.toTsvString)

      ctr += 1
      if (ctr % 100000 == 0)
        LOG.info(ctr + " occs processed!")
    })

    writers.foreach(_._2.close())
    otherWriter.close()
  }
}
