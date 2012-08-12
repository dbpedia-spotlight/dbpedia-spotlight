package org.dbpedia.spotlight.topic.wikipedia.flattening

import org.dbpedia.spotlight.model.{TopicDescription, DBpediaCategory, Topic}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.topic.wikipedia.util.WikipediaFlattenedHierarchyLoader
import java.io.{File, FileWriter, PrintWriter}
import scala.collection.mutable._
import org.dbpedia.spotlight.db.model.WordIdDictionary
import org.dbpedia.spotlight.util.TextVectorizer
import org.dbpedia.spotlight.topic.convert.VowpalToArff
import org.dbpedia.spotlight.topic.WekaSingleLabelClassifier
import java.util.regex.Pattern
import org.dbpedia.spotlight.util.IndexingConfiguration

/**
 * This object calculates topic assignments for dbpedia categories. Those topics and their specific keywords (important) are defined
 * in the org.dbpedia.spotlight.topic.description file which can be defined in the indexing properties. The result will be a flattened
 * category hierarchy which can be used as input for SplitOccsByCategories$. Input are the corpora created by ExtractCategoryCorpus.
 * Training corpus is the corpus whose elements (if assigned) will be used for training the classifier and the evaluation
 * corpus is the corpus whose elements will be assigned to topics if possible. Training and evaluation corpus can (and most
 * of the time should) be the same. The 'rest'-corpus that was created by ExtractCategoryCorpus should never be the training
 * corpus because it contains only sparse categories.
 *
 * @author Dirk Weissenborn
 */

object FlattenHierarchySemiSupervised {
    private val LOG = LogFactory.getLog(getClass)

    /**
     *
     * @param args  path to indexing properties, path to training corpus, path to training input categories,
     *              path to evaluation corpus, path to evaluation corpus' categories, path to temporary dir,
     *              confidence threshold for assigning a category to a topic (should be high, prob. at least 0.8)
     */
    def main(args: Array[String]) {
        val config = new IndexingConfiguration(args(0))

        flattenHierarchyByTopics(
            new File(config.get("org.dbpedia.spotlight.topic.description")),
            new File(args(1)),
            new File(args(2)),
            new File(args(3)),
            new File(args(4)),
            new File(config.get("org.dbpedia.spotlight.topic.categories.dictionary")),
            new File(config.get("org.dbpedia.spotlight.data.concepts")),
            new File(args(5)),
            new File(config.get("org.dbpedia.spotlight.topic.flattenedHierarchy")),
            args(6).toDouble
        )
    }

    def flattenHierarchyByTopics(topicDescriptionFile: File,
                                 trainingCorpus: File, //input
                                 trainingCorpusCategories: File, //input.categories
                                 evaluationCorpus: File, //rest
                                 evaluationCorpusCategories: File, //rest.categories
                                 dictionaryFile: File,
                                 topicalConceptsFile: File,
                                 tmpFile: File,
                                 output: File,
                                 classificationThreshold: Double) {

        def getScore(categoryName: Set[String], matchName: String): Double = {
            val parts = matchName.split("_")
            parts.foreach(part => if (!categoryName.contains(part)) return 0.0)

            return 1.0
        }

        output.mkdirs()

        //val concepts = TopicalConceptLoader.loadTopicalConcepts(topicalConceptsFile)

        val tempArffPath = tmpFile + "/topics.arff"
        val tempTopicsPath = tmpFile + "/topics.list"
        val tempModelPath = tmpFile + "/model.dat"
        val normalize = true

        val dictionary = new WordIdDictionary(dictionaryFile)
        dictionary.setMaxSize(dictionary.getSize)
        var topicCategories = WikipediaFlattenedHierarchyLoader.loadFlattenedHierarchy(output).transform((topic, categories) => categories.transform((category, distance) => 1.0 / distance)) //Map[Topic,Map[DBpediaCategory,Double]]()
        val alreadyProcessed = Set[DBpediaCategory]()

        val vectorizer = new TextVectorizer()

        //do not allow years as categories, e.g. 1830_births, 1830_deaths -> too many of them - bring a lot of confusion, People_by People_from
        LOG.info("Categories starting with a year or consisting of the word 'people' will not be assigned!")
        val pattern = Pattern.compile("(\\d\\d+|People.*|.*people|.*expatriates).*").matcher("")

        val topicDescriptions = TopicDescription.fromDescriptionFile(topicDescriptionFile)

        // if no categories were assigned yet, find trivial assignments (by keyword matching)
        if (topicCategories.isEmpty) {
            topicDescriptions.foreach(description => {
                topicCategories += (description.topic -> Map[DBpediaCategory, Double]())
            })

            var assignedCategoriesCtr = 0
            scala.io.Source.fromFile(trainingCorpusCategories).getLines().foreach(cat => {
                val category = new DBpediaCategory(cat)
                pattern.reset(category.getCategory)
                if (!alreadyProcessed.contains(category) && !pattern.matches()) {
                    val catNameAsSet = Set() ++ vectorizer.getWordCountVector(category.getCategory.toLowerCase.replaceAll("[^a-z]", " ")).keySet
                    val selectedTopics = Map[Topic, Double]()

                    topicDescriptions.foreach(description => {
                        var sum = 0.0
                        description.keywords.foreach(keyword => sum += getScore(catNameAsSet, keyword))

                        if (sum >= 1.0) {
                            selectedTopics += (description.topic -> sum)
                        }
                    })

                    if (selectedTopics.size > 0) {
                        var topics = selectedTopics.toList.sortBy(x => -x._2)
                        topics = topics.takeWhile {
                            case (topic, value) => value > 0.9 * topics.head._2
                        }
                        topics.foreach(topic => {
                            topicCategories(topic._1) += (category -> topic._2)
                        })
                        assignedCategoriesCtr += 1
                        alreadyProcessed += (category)
                        if (assignedCategoriesCtr % 1000 == 0) {
                            LOG.info("Assigned " + assignedCategoriesCtr + " categories to topics")
                            LOG.info("Latest assignment: " + category.getCategory + " -> " + topics.head._1.getName)
                        }
                    }
                }
            })

            val writers = Map[Topic, PrintWriter]()
            topicDescriptions.foreach(description => writers += (description.topic -> new PrintWriter(new FileWriter(output + "/" + description.topic.getName + ".tsv"))))

            topicCategories.foreach {
                case (topic, categories) => {
                    categories.toList.sortBy(-_._2).foreach {
                        case (category, score) => writers(topic).println(category.getCategory + "\t" + 1 / score)
                    }
                }
            }

            writers.foreach(_._2.close())
        }
        else
            topicCategories.foreach {
                case (topic, categories) => alreadyProcessed ++= categories.keySet
            }


        LOG.info(alreadyProcessed.size + " categories were already assigned")

        LOG.info("Assigning categories to topics utilizing topical classification")

        val topicCategoriesForTraining = Map[Topic, Set[DBpediaCategory]]()
        scala.io.Source.fromFile(trainingCorpusCategories).getLines().foreach(category => {
            topicCategories.foreach {
                case (topic, categories) =>
                    if (categories.keySet.contains(new DBpediaCategory(category))) {
                        if (topicCategoriesForTraining.contains(topic))
                            topicCategoriesForTraining(topic) += (new DBpediaCategory(category))
                        else
                            topicCategoriesForTraining += (topic -> Set(new DBpediaCategory(category)))
                    }
            }
        })

        val nrOfExamples = topicCategoriesForTraining.foldLeft(Double.MaxValue)((acc, topicCats) => math.min(acc, topicCats._2.size))

        topicCategories.foreach {
            case (topic, cats) =>
                topicCategoriesForTraining.update(topic,
                    Set[DBpediaCategory]() ++
                        cats.filter(category => topicCategoriesForTraining(topic).contains(category._1)).toList.sortBy(-_._2).take(nrOfExamples.toInt).map(_._1).toSet
                )
        }

        LOG.info("Writing new corpus for training topical classifier")
        //SplitOccsByCategories$.splitOccs(pathToTempFlattenedHierarchy,pathToSortedArticlesCategories,pathToSortedOccs,pathToTempSplittedOccs)
        val tmpTopicsWriter = new PrintWriter(new FileWriter(tempTopicsPath))
        tmpTopicsWriter.println(topicCategories.keySet.toList.map(_.getName).sorted.reduceLeft(_ + "," + _))
        scala.io.Source.fromFile(trainingCorpusCategories).getLines().foreach(category => {
            var topicsString = ""
            topicCategoriesForTraining.foreach {
                case (topic, categories) =>
                    if (categories.contains(new DBpediaCategory(category))) {
                        topicsString += "," + topic.getName
                    }
            }
            if (topicsString.isEmpty)
                topicsString = ",_ignore"
            tmpTopicsWriter.println(topicsString.substring(1))
        })

        tmpTopicsWriter.close()

        VowpalToArff.writeVowpalToArff(dictionary, trainingCorpus, new File(tempTopicsPath), new File(tempArffPath), normalize)

        LOG.info("Training model")
        WekaSingleLabelClassifier.trainModel(new File(tempArffPath), new File(tempModelPath))

        LOG.info("Assign categories to topics with new model")
        val classifier = new WekaSingleLabelClassifier(dictionary, new File(tempModelPath), topicCategories.keySet.toList)

        val lines = scala.io.Source.fromFile(evaluationCorpus).getLines()
        val topicLines = scala.io.Source.fromFile(evaluationCorpusCategories).getLines()

        var ctr = 0

        lines.foreach((fileLine) => {
            val category = new DBpediaCategory(topicLines.next())
            pattern.reset(category.getCategory)
            if (!alreadyProcessed.contains(category) && !pattern.matches()) {
                // Try first trivial assignments using keyword matching
                val catNameAsSet = Set() ++ vectorizer.getWordCountVector(category.getCategory.toLowerCase.replaceAll("[^a-z]", " ")).keySet
                val selectedTopics = Map[Topic, Double]()

                topicDescriptions.foreach(description => {
                    var sum = 0.0
                    description.keywords.foreach(keyword => sum += getScore(catNameAsSet, keyword))

                    if (sum >= 1.0) {
                        selectedTopics += (description.topic -> sum)
                    }
                })

                if (selectedTopics.size > 0) {
                    var topics = selectedTopics.toList.sortBy(x => -x._2)
                    topics = topics.takeWhile {
                        case (topic, value) => value > 0.9 * topics.head._2
                    }
                    topics.foreach(topic => {
                        topicCategories(topics.head._1) += (category -> topics.head._2)
                    })
                    ctr += 1
                    if (ctr % 100 == 0) {
                        LOG.info("Assigned " + ctr + " new categories to topics")
                        LOG.info("Latest assignment: " + category.getCategory + " -> " + topics.head._1.getName)
                    }
                }
                // if category could not be assigned trivially, try assigning with trained classifier
                else if (classificationThreshold < 1.0) {
                    var values = List[(Int, Double)]()

                    val split = fileLine.split(" ")

                    if (split.length > 2000) {
                        var word: Array[String] = null
                        var wordId = -1
                        for (i <- 1 until split.length) {
                            word = split(i).split(":")

                            wordId = word(0).toInt
                            if (wordId > -1) {
                                values = (wordId, word(1).toDouble) :: values
                            }
                        }
                        //length normalization
                        var squaredSum = 1.0
                        if (normalize)
                            squaredSum = math.sqrt(values.foldLeft(0.0)((acc, element) => acc + element._2 * element._2))

                        values = values.sortBy(_._1)

                        val result = classifier.getPredictions(values.map[Int, List[Int]](element => element._1).toArray, values.map[Double, List[Double]](element => element._2 / squaredSum).toArray)

                        result.foreach {
                            case (topic, score) => {
                                if (score >= classificationThreshold) {
                                    topicCategories(topic) += (category -> score)
                                    ctr += 1
                                    if (ctr % 100 == 0) {
                                        LOG.info("Assigned " + ctr + " new categories to topics")
                                        LOG.info("Latest assignment: " + category.getCategory + " -> " + topic.getName)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })

        val writers = Map[Topic, PrintWriter]()
        topicDescriptions.foreach(description => writers += (description.topic -> new PrintWriter(new FileWriter(output + "/" + description.topic.getName + ".tsv"))))

        topicCategories.foreach {
            case (topic, categories) => {
                categories.toList.sortBy(-_._2).foreach {
                    case (category, score) => writers(topic).println(category.getCategory + "\t" + 1 / score)
                }
            }
        }

        writers.foreach(_._2.close())
    }

}
