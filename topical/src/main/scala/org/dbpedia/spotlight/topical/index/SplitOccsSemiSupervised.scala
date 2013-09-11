package org.dbpedia.spotlight.topical.index

import java.io.{FileWriter, PrintWriter, File}
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.util.IndexingConfiguration
import org.dbpedia.spotlight.topical.{TopicalClassifier, TopicalClassifierTrainer}
import org.dbpedia.spotlight.topical.util.TopicUtil
import org.dbpedia.spotlight.io.FileOccurrenceSource
import org.dbpedia.spotlight.log.SpotlightLog
import collection.mutable._
import com.sun.grizzly.util.FileUtil
import org.apache.commons.io.FileUtils
import io.Source

/**
 * This object splits the occs file into several topical occs files, by first creating an initial split, which is done
 * by defining main categories for each topic in the topic.description file (which location is specified
 * in the indexing.properties, can usually be found in the conf/ folder) and assigning each resource which are members of one of these main categories
 * to the specific topics or, if the output directory is not empty, taking its content as the initial split (allowing
 * this procedure to be run several times with different thresholds, e.g. first run with 0.8 threshold and 1 iteration and second run with 0.5 and 1 iteration).
 * The initial split is afterwards used to train an initial topical classifier which is then used to assign unassigned occs to the topics.
 * This step can be repeated several times (splitting, training a model on the new split, splitting again ...) by defining a number of iterations.
 * </br></br>
 * Threshold example statistics on initial split (after training on initial split): </br>
 * cutoff-number of examples within this cutoff-accuracy </br>
 * 0.01 - 1.0 - 0.66  </br>
 * 0.1 - 0.97 - 0.67  </br>
 * 0.2 = 0.73 - 0.74  </br>
 * 0.3 = 0.52- 0.8   </br>
 * 0.4 = 0.38 - 0.85  </br>
 * 0.5 = 0.29 - 0.89  </br>
 * @author dirk
**/

//TODO just allow concept uris
object SplitOccsSemiSupervised {
    /**
     *
     * @param args 1st: indexing.properties 2nd: path to occs file, 3rd: temporary path (same partition as output)
     *             , 4th: minimal confidence of assigning an occ to a topic (see
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
            SpotlightLog.error(this.getClass, "Insufficient arguments!")
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
        val toSplit = new File(tmpDir, "toSplit.tsv")
        val trainingDir = new File(outputDir.getAbsolutePath+"-training")

        outputDir.mkdirs()
        trainingDir.mkdirs()

        if (outputDir.listFiles().size > 0) {
            SpotlightLog.info(this.getClass, "Output directory was not empty. Taking split in this directory as initial split.")
            new File(outputDir, TopicUtil.CATCH_TOPIC.getName + ".tsv").renameTo(toSplit)
            outputDir.renameTo(trainingDir)
        }
        else {
            SpotlightLog.info(this.getClass, "Creating initial split for training an initial model for splitting!")
            initialSplit(topicDescriptionFile, articleCatsFile, occsFile, trainingDir)
        }

        val trainer = TopicalClassifierTrainer.byType(classifierType)
        var classifier:TopicalClassifier =  null

        for (i <- 0 until iterations) {
            GenerateOccTopicCorpus.generateCorpusWithEqualCount(trainingDir, tmpCorpus)

            /*val corpus = trainingDir.listFiles().iterator.flatMap(topicFile => {
                val topic = new Topic(topicFile.getName.substring(0, topicFile.getName.length - 4))
                if (!topic.getName.equals(TopicUtil.CATCH_TOPIC)) {
                    FileOccurrenceSource.fromFile(topicFile).map(occ => {
                        (topic, occ.context)
                    })
                }
                else
                    List[(Topic,Text)]().iterator
            })  */

            if (trainer.needsShuffled) {
                SpotlightLog.info(this.getClass, "Shuffling corpus!")
                FileUtils.moveFile(tmpCorpus, new File(tmpCorpus.getAbsolutePath+ ".tmp"))
                new ProcessBuilder("sort", "-R", "-o", tmpCorpus.getAbsolutePath, tmpCorpus.getAbsolutePath + ".tmp").start().waitFor()
                new File(tmpCorpus.getAbsolutePath + ".tmp").delete()
            }
            if (i == 0)
                classifier = trainer.trainModel(tmpCorpus,10)
            else
                trainer.trainModelIncremental(tmpCorpus,10,classifier)

            // After first split only take assigned occs for training, otherwise merge new training examples with old training examples
            if(i == 1){
                outputDir.listFiles().foreach(_.delete())
                outputDir.delete()
                trainingDir.renameTo(outputDir)
            }
            else
                mergeDirectories(trainingDir,outputDir)

            val f = if(i<2) occsFile else toSplit

            SpotlightLog.info(this.getClass, "Start splitting occs into topics, iteration: %d", i)
            if (i < iterations-1) {
                trainingDir.mkdirs()
                AssignTopicsToOccs.assignTopics(f, classifier, threshold, trainingDir, false)

                new File(trainingDir, TopicUtil.CATCH_TOPIC.getName + ".tsv").renameTo(toSplit)
            }
            else
                AssignTopicsToOccs.assignTopics(f, classifier, threshold, outputDir, i > 1)
        }

        tmpDir.listFiles().foreach(_.delete())
    }

    private def mergeDirectories(deleteDir:File, keepDir:File) {
        keepDir.mkdirs()
        deleteDir.mkdirs()
        deleteDir.listFiles().foreach(f => {
            keepDir.listFiles().find(_.getName == f.getName) match {
                case Some(otherF) => {
                    val pw = new PrintWriter(new FileWriter(otherF,true))
                    Source.fromFile(f).getLines().foreach(l => pw.println(l))
                    pw.close()
                }
                case None => {
                    val pw = new PrintWriter(new FileWriter(new File(keepDir,f.getName)))
                    Source.fromFile(f).getLines().foreach(l => pw.println(l))
                    pw.close()
                }
            }

        })

        deleteDir.listFiles().foreach(_.delete())
        deleteDir.delete()
    }

    private def loadArticleCategories(articleCats: File, descriptions: collection.Seq[TopicDescription]): Map[DBpediaResource, Set[Topic]] = {
        val assignments = Map[DBpediaResource, Set[Topic]]()

        val categoryAssignments =
            descriptions.
                flatMap(d => d.categories.map(c => (new DBpediaCategory(c),d.topic))).
                groupBy(_._1).map(tuple => (tuple._1,tuple._2.map(_._2).toSet)).toMap

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
                    SpotlightLog.info(this.getClass, "Assigned %d occs to topics", assignedResourcesCtr)
                    SpotlightLog.info(this.getClass, "Latest assignment: %s -> %s", lastResource.uri, selectedTopics.foldLeft("")(_ + " " + _.getName))
                }
            }

            ctr += 1
            if (ctr % 10000 == 0)
                SpotlightLog.info(this.getClass, "%d occs processed!", ctr)
        })

        writers.foreach(_._2.close())
    }
}