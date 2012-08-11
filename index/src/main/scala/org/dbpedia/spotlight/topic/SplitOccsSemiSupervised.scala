package org.dbpedia.spotlight.topic

import scala.collection.mutable.Map
import scala.collection.mutable.Set
import java.io.{FileWriter, PrintWriter, File}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.io.FileOccurrenceSource
import org.dbpedia.spotlight.model.{DBpediaCategory, TopicDescription, DBpediaResource, Topic}
import org.dbpedia.spotlight.util.{IndexingConfiguration}
import org.dbpedia.spotlight.db.model.{TopicalStatInformation, WordIdDictionary}
import org.dbpedia.spotlight.topic.util.TopicUtil
import org.dbpedia.spotlight.topic.convert.TextCorpusToInputCorpus
import io.Source

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
            splitOccs(new File(args(1)),
                new File(config.get("org.dbpedia.spotlight.topic.description")),
                new File(config.get("org.dbpedia.spotlight.data.sortedArticlesCategories")),
                new File(config.get("org.dbpedia.spotlight.data.categories")) , new File(args(2)), args(3).toDouble, args(4).toInt, new File(args(5)))
        }
        else
            LOG.error("Not sufficient arguments!")
    }

    def splitOccs(occsFile: File,
                  topicDescriptionFile: File,
                  articleCatsFile: File,
                  wikiCategoriesFile: File,
                  tmpDir: File, threshold: Double, iterations: Int, outputFile: File) {
        tmpDir.mkdirs()
        val tmpCorpus = new File(tmpDir, "tmpCorpus.tsv")
        val tmpTopics = new File(tmpDir, "tmpTopics.info")
        val tmpDic = new File(tmpDir, "tmpDic.dic")
        val tmpArff = new File(tmpDir, "tmpArff.arff")
        val tmpModel = new File(tmpDir, "tmpModel")
        val tmpOther = new File(tmpDir, "tmpOther.tsv")

        outputFile.mkdirs()

        if(outputFile.listFiles().size>0) {
            LOG.info("Output directory was not empty. Taking split in this directory as initial split.")
            new File(outputFile, TopicUtil.CATCH_TOPIC.getName+".tsv").renameTo(tmpOther)
        }
        else {
            LOG.info("Creating initial split for training an initial model for splitting!")
            initialSplit(topicDescriptionFile,articleCatsFile, wikiCategoriesFile, occsFile, outputFile)
        }

        for (i <- 0 until iterations) {
            GenerateOccTopicCorpus.generateCorpus(outputFile, -1, new File(tmpCorpus.getAbsolutePath + ".tmp"))
            new ProcessBuilder("sort", "-R", "-o", tmpCorpus.getAbsolutePath,  tmpCorpus.getAbsolutePath + ".tmp").start().waitFor()
            new File(tmpCorpus + ".tmp").delete()

            TextCorpusToInputCorpus.writeDocumentsToCorpus(tmpCorpus, tmpArff, false, true, "arff", tmpTopics, tmpDic, 100000, 100000)
            WekaSingleLabelClassifier.trainModel(tmpArff, tmpModel)
            val dictionary = new WordIdDictionary(tmpDic)
            val classifier = new WekaSingleLabelClassifier(dictionary, new TopicalStatInformation(tmpTopics), tmpModel)

            if (i==0) {
                AssignTopicsToOccs.assignTopics(occsFile, classifier, threshold, outputFile, false)
            }
            else
                AssignTopicsToOccs.assignTopics(tmpOther, classifier, threshold, outputFile, true)

            tmpOther.delete()
            if (i < iterations - 1)
                new File(outputFile, TopicUtil.CATCH_TOPIC.getName+".tsv").renameTo(tmpOther)
        }

        //new File(tmpFile).delete()
    }

    def load(articleCats:File, descriptions: Seq[TopicDescription], hierarchy:Map[DBpediaCategory, Set[DBpediaCategory]]) : Map[DBpediaResource,Set[Topic]] = {
        var assignments = Map[DBpediaResource,Set[Topic]]()

        val categoryAssignments = descriptions.foldLeft(Map[DBpediaCategory,Set[Topic]]())((acc, description) =>
            acc ++ (description.categories.foldLeft(Map[DBpediaCategory,Set[Topic]]())( (acc2, category) => {
                val cat = new DBpediaCategory(category)
                var result = Map() ++ acc

                if(!result.contains(cat))
                    result += (cat -> Set(description.topic))
                else
                    result(cat) += (description.topic)

                /*
                hierarchy.getOrElse(cat,Set[DBpediaCategory]()).foreach( subCat => {
                    if(result.contains(subCat)) {
                        result(subCat) += (description.topic)
                    }
                    else  {
                        result += (subCat -> Set(description.topic))
                    } : Unit

                })*/

                result
            })))

        // LOG.info(categoryAssignments.foldLeft("") ((s,entry) => s+ entry._1.getCategory+" -> "+ entry._2.foldLeft("")(_+" "+_.getName)+"\n"))

        Source.fromFile(articleCats).getLines().foreach( line => {
            val split = line.split(" ")
            val category = new DBpediaCategory(split(2).substring(1, split(2).length-1) )
            if (categoryAssignments.contains(category))  {
                val resource = new DBpediaResource(split(0).substring(1, split(0).length-1))

                assignments.getOrElseUpdate(resource, Set[Topic]()) ++= (categoryAssignments(category))
            }

        })

        assignments
    }

    def initialSplit(topicDescriptionFile: File,
                     articleCatsFile:File,
                     wikiCategoriesFile:File,
                     occsFile: File,
                     output: File) {
        def getScore(categoryName: Set[String], matchName: String): Double = {
            val parts = matchName.split("_")
            parts.foreach(part => if (!categoryName.contains(part)) return 0.0)

            return 1.0
        }

        //val hierarchy = WikipediaHierarchyLoader.loadCategoryHierarchy(wikiCategoriesFile.getAbsolutePath)


        output.mkdirs()
        val writers = Map[Topic, PrintWriter]()
        val topicDescriptions = TopicDescription.fromDescriptionFile(topicDescriptionFile)
        //val vectorizer = new TextVectorizer()

        val initialAssignments = load(articleCatsFile, topicDescriptions,  null)

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

                /*/val resourceNameAsSet = Set() ++ vectorizer.getWordCountVector(lastResource.uri.toLowerCase.replaceAll("[^a-z]", " ")).keySet
                topicDescriptions.foreach(topicInfo => {
                    var sum = 0.0
                    topicInfo.keywords.foreach(keyword => sum += getScore(resourceNameAsSet, keyword))

                    if (sum >= 1.0) {
                        selectedTopics += (topicInfo.topic -> sum)
                    }

                })*/

                selectedTopics = initialAssignments.getOrElse(lastResource, Set[Topic]())

            }

            if (selectedTopics.size > 0) {
                /*var topics = selectedTopics.toList.sortBy(x => -x._2)
                topics = topics.takeWhile {
                    case (topic, value) => value > 0.9 * topics.head._2
                } */
                selectedTopics.foreach(topic => {
                    writers(topic).println(occ.toTsvString)
                })
                assignedResourcesCtr += 1
                if (assignedResourcesCtr % 10000 == 0) {
                    LOG.info("Assigned " + assignedResourcesCtr + " occs to topics")
                    LOG.info("Latest assignment: " + lastResource.uri + " -> " + selectedTopics.foldLeft("")(_ + " "+_.getName))
                }
            }
            /*else
                restWriter.println(occ.toTsvString) */

            ctr += 1
            if (ctr % 100000 == 0)
                LOG.info(ctr + " occs processed!")
        })

        writers.foreach(_._2.close())
    }
}
