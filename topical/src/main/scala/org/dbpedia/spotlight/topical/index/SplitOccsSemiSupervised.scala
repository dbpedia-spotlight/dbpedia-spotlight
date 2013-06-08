package org.dbpedia.spotlight.topical.index

import java.io.{FileWriter, PrintWriter, File}
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.util.IndexingConfiguration
import org.dbpedia.spotlight.topical.{TopicalClassifierTrainer}
import org.dbpedia.spotlight.topical.util.TopicUtil
import org.dbpedia.spotlight.io.FileOccurrenceSource
import org.apache.commons.logging.LogFactory
import collection.mutable._

/**
 * This object splits the occs file into several topical occs files, by first creating an initial split, which is done
 * by defining main categories for each topic in the topic.description file (which location is specified
 * in the indexing.properties, can usually be found in the conf/ folder) and assigning each resource which are members of one of these main categories
 * to the specific topics. After that, all occs of these assigned resources are assigned to the specific topics as well.
 * The initial split is afterwards used to train an initial topical classifier which is then used to assign occs to the topics.
 * This step can be repeated several times (splitting, training a model on the new split, splitting again ...)
 *
 * @author dirk
 */
//TODO just allow concept uris
object SplitOccsSemiSupervised {
    private val LOG = LogFactory.getLog(getClass)

    /**
     *
     * @param args 1st: indexing.properties 2nd: path to occs file, 3rd: temporary path (same partition as output)
     *             , 4th: minimal confidence of assigning an occ to a topic (0.5 has experimentally shown to be be good for naive bayes with 0.9 accuracy on the initial split)
     *             , 5th: nr of iterations, 6th: path to output directory
     *
     */
    def main(args: Array[String]) {
        if (args.length > 5) {
            val config = new IndexingConfiguration(args(0))
            splitOccs(new File(args(1)),
                config.get(TopicalClassificationConfiguration.CLASSIFIER_TYPE),
                new File(config.get(TopicalClassificationConfiguration.TOPIC_DESCRIPTION)),
                new File(config.get("org.dbpedia.spotlight.data.sortedArticlesCategories")),
                new File(args(2)),
                args(3).toDouble,
                args(4).toInt,
                new File(args(5)))
        }
        else
            LOG.error("Insufficient arguments!")
    }

    def splitOccs(occsFile: File,
                  classifierType:String,
                  topicDescriptionFile: File,
                  articleCatsFile: File,
                  tmpDir: File,
                  threshold: Double,
                  iterations: Int,
                  outputDir: File) {
        tmpDir.mkdirs()
        val tmpCorpus = new File(tmpDir, "corpus.tsv")
        val tmpOther = new File(tmpDir, "toSplit.tsv")

        outputDir.mkdirs()

        if (outputDir.listFiles().size > 0) {
            LOG.info("Output directory was not empty. Taking split in this directory as initial split.")
            new File(outputDir, TopicUtil.CATCH_TOPIC.getName + ".tsv").renameTo(tmpOther)
        }
        else {
            LOG.info("Creating initial split for training an initial model for splitting!")
            initialSplit(topicDescriptionFile, articleCatsFile, occsFile, outputDir)
        }

        val trainer = TopicalClassifierTrainer.byType(classifierType)

        for (i <- 0 until iterations) {
            GenerateOccTopicCorpus.generateCorpus(outputDir, -1, new File(tmpCorpus.getAbsolutePath + ".tmp"))
            new ProcessBuilder("sort", "-R", "-o", tmpCorpus.getAbsolutePath, tmpCorpus.getAbsolutePath + ".tmp").start().waitFor()
            new File(tmpCorpus + ".tmp").delete()

            val classifier =  trainer.trainModel(tmpCorpus,10)

            if (i == 0) {
                AssignTopicsToOccs.assignTopics(occsFile, classifier, threshold, outputDir, false)
            }
            else
                AssignTopicsToOccs.assignTopics(tmpOther, classifier, threshold, outputDir, true)

            tmpOther.delete()
            if (i < iterations - 1)
                new File(outputDir, TopicUtil.CATCH_TOPIC.getName + ".tsv").renameTo(tmpOther)
        }

        tmpDir.listFiles().foreach(_.delete())
    }

    def loadArticleCategories(articleCats: File, descriptions: collection.Seq[TopicDescription]): Map[DBpediaResource, Set[Topic]] = {
        val assignments = Map[DBpediaResource, Set[Topic]]()

        val categoryAssignments = descriptions.foldLeft(Map[DBpediaCategory, Set[Topic]]())((acc, description) =>
            acc ++ (description.categories.foldLeft(Map[DBpediaCategory, Set[Topic]]())((acc2, category) => {
                val cat = new DBpediaCategory(category)
                var result = Map() ++ acc2

                if (!result.contains(cat))
                    result += (cat -> Set(description.topic))
                else
                    result(cat) += (description.topic)

                result
            })))

        scala.io.Source.fromFile(articleCats).getLines().foreach(line => {
            val split = line.split(" ")
            val category = new DBpediaCategory(split(2).substring(1, split(2).length - 1))
            if (categoryAssignments.contains(category)) {
                val resource = new DBpediaResource(split(0).substring(1, split(0).length - 1))

                assignments.getOrElseUpdate(resource, Set[Topic]()) ++= (categoryAssignments(category))
            }

        })

        assignments
    }

    def initialSplit(topicDescriptionFile: File,
                     articleCatsFile: File,
                     occsFile: File,
                     output: File) {
        output.mkdirs()
        val writers = Map[Topic, PrintWriter]()
        val topicDescriptions = TopicDescription.fromDescriptionFile(topicDescriptionFile)

        val initialAssignments = loadArticleCategories(articleCatsFile, topicDescriptions)

        topicDescriptions.foreach(description => {
            writers += (description.topic -> new PrintWriter(new FileWriter(new File(output, description.topic.getName + ".tsv"))))
        })
        var assignedResourcesCtr = 0
        var ctr = 0

        var lastResource: DBpediaResource = new DBpediaResource("")
        var selectedTopics = Set[Topic]()
        FileOccurrenceSource.fromFile(occsFile).foreach(occ => {
            if (!occ.resource.equals(lastResource)) {
                lastResource = occ.resource
                selectedTopics = initialAssignments.getOrElse(lastResource, Set[Topic]())
            }

            if (selectedTopics.size > 0) {
                selectedTopics.foreach(topic => {
                    writers(topic).println(occ.toTsvString)
                })
                assignedResourcesCtr += 1
                if (assignedResourcesCtr % 10000 == 0) {
                    LOG.info("Assigned " + assignedResourcesCtr + " occs to topics")
                    LOG.info("Latest assignment: " + lastResource.uri + " -> " + selectedTopics.foldLeft("")(_ + " " + _.getName))
                }
            }

            ctr += 1
            if (ctr % 100000 == 0)
                LOG.info(ctr + " occs processed!")
        })

        writers.foreach(_._2.close())
    }
}
