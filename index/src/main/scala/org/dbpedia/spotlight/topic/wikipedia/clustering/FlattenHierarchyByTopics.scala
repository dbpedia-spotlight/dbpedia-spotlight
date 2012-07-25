package org.dbpedia.spotlight.topic.wikipedia.clustering

import org.dbpedia.spotlight.model.{DBpediaCategory, DBpediaTopic}
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.topic.wikipedia.util.{WikipediaFlattenedHierarchyLoader, WikipediaTopicVectorLoader, WikipediaHierarchyLoader}
import java.io.{File, FileWriter, PrintWriter}
import scala.collection.mutable._
import org.dbpedia.spotlight.topic.utility.{TextVectorizer, WordIdDictionary}
import org.dbpedia.spotlight.topic.convert.VowpalToArff
import org.dbpedia.spotlight.topic.NaiveBayesMultinomialClassifier
import collection.mutable
import java.util.regex.Pattern
import io.Source
import org.dbpedia.spotlight.util.IndexingConfiguration

/**
 * This object calculates topic assignments for dbpedia categories. Those topics and their specific keywords(important) are defined
 * in the org.dbpedia.spotlight.topic.keywords file which can be defined in the indexing properties. The result will be a flattened
 * category hierarchy which can be used as input for SplitOccsByTopics.
 *
 * @author Dirk Weissenborn
 */

object FlattenHierarchyByTopics {
  private val LOG = LogFactory.getLog(getClass)

  /**
   *
   * @param args  path to indexing properties, path to training corpus, path to training input categories,
   *              path to evaluation corpus, path to evaluation corpus' categories, path to temporary dir,
   *              confidence threshold for assigning a category to a topic (should be high, prob. at least 0.8)
   */
  def main(args:Array[String]) {
    /*flattenHierarchyByTopics(
      "/home/dirk/workspace/dbpedia-spotlight/index/src/main/resources/topics.list",
      "/media/Data/Wikipedia/skos_categories_en.nt",
      "/media/Data/Wikipedia/Clustering/reduced.input.vowpal",
      "/media/Data/Wikipedia/Clustering/categories.list",
      "/media/Data/Wikipedia/Clustering/reduced.input.vowpal",
      "/media/Data/Wikipedia/Clustering/categories.list",
      "/media/Data/Wikipedia/Dictionary/cluster.topic.dict",
      "/media/DirksHDD/GSOC2012/tmp",
      "/media/Data/Wikipedia/FlattenedHierarchyByTopics",
      0.8
      )*/
    val config = new IndexingConfiguration(args(0))

    flattenHierarchyByTopics(
      config.get("org.dbpedia.spotlight.topic.keywords"),
      config.get("org.dbpedia.spotlight.data.categories"),
      args(1),
      args(2),
      args(3),
      args(4),
      config.get("org.dbpedia.spotlight.topic.categories.dictionary"),
      args(5),
      config.get("org.dbpedia.spotlight.topic.flattenedHierarchy"),
      args(6).toInt
    )
  }

  def flattenHierarchyByTopics(pathToTopicKeywords:String,
                               pathToWikiCategories:String,
                               pathToTrainingCorpus:String,
                               pathToTrainingCorpusCategories:String,
                               pathToEvaluationCorpus:String,
                               pathToEvaluationCorpusCategories:String,
                               pathToDictionary:String,
                               tmpPath:String,
                               output:String,
                               classificationThreshold:Double)  {

    def getScore(categoryName:Set[String], matchName:String) : Double = {
      val parts = matchName.split("_")
      parts.foreach( part => if(!categoryName.contains(part)) return 0.0 )

      return 1.0
    }

    new File(output).mkdirs()

    val tempArffPath = tmpPath+"/topics.arff"
    val tempTopicsPath = tmpPath+"/topics.list"
    val tempModelPath = tmpPath+"/model.dat"
    val normalize = true

    val dictionary = new WordIdDictionary(pathToDictionary)
    var topicCategories = WikipediaFlattenedHierarchyLoader.loadFlattenedHierarchy(output).transform( (topic, categories) => categories.transform( (category,distance) => 1.0/distance) )//Map[DBpediaTopic,Map[DBpediaCategory,Double]]()
    val alreadyProcessed = Set[DBpediaCategory]()
    val topicKeywords = Map[DBpediaTopic,Set[String]]()

    val vectorizer = new TextVectorizer()

    Source.fromFile(pathToTopicKeywords).getLines().foreach( topicDescription => {
      val lineSplit = topicDescription.split("=")
      val set = Set[String]()
      set += (vectorizer.getWordCountVector(lineSplit(0).replace("_"," ")).keySet.reduceLeft(_+"_"+_))

      val keywords = lineSplit(2).split(",")
      keywords.foreach( word => { set += (vectorizer.getWordCountVector(word.replace("_"," ")).keySet.reduceLeft(_+"_"+_)) } )

      topicKeywords += (new DBpediaTopic(lineSplit(0).trim) -> set)
    } )

    if (topicCategories.isEmpty) {
      topicKeywords.foreach( topic => {
        topicCategories += (topic._1 -> Map[DBpediaCategory,Double]())
      } )

      var wikiHierarchy = WikipediaHierarchyLoader.loadCategoryHierarchy(pathToWikiCategories)

      var assignedCategoriesCtr = 0
      wikiHierarchy.foreach { case (category, children) =>
        if (!alreadyProcessed.contains(category)) {
          val catNameAsSet = Set() ++ vectorizer.getWordCountVector(category.getCategory.toLowerCase.replaceAll("[^a-z]", " ")).keySet
          val selectedTopics = Map[DBpediaTopic, Double]()

          topicKeywords.foreach { case (topic,keywords) => {
            var sum = 0.0
            keywords.foreach( keyword => sum += getScore(catNameAsSet,keyword) )

            if (sum >=1.0) {
              selectedTopics += (topic -> sum)
            }
          }}

          if (selectedTopics.size > 0) {
            var topics = selectedTopics.toList.sortBy(x => -x._2)
            topics = topics.takeWhile { case (topic, value) => value > 0.9*topics.head._2 }
            topics.foreach(topic => {
              topicCategories(topics.head._1) += (category -> topics.head._2)
            })
            assignedCategoriesCtr += 1
            alreadyProcessed += (category)
            if(assignedCategoriesCtr%1000==0) {
              LOG.info("Assigned "+assignedCategoriesCtr+" categories to topics")
              LOG.info("Latest assignment: " + category.getCategory + " -> " + topics.head._1.getTopic)
            }
          }
        }
      }
      wikiHierarchy = null

      val writers = Map[DBpediaTopic,PrintWriter]()
      topicKeywords.foreach{ case (topic,keywords) => writers += (topic -> new PrintWriter(new FileWriter(output+"/"+topic.getTopic+".tsv"))) }

      topicCategories.foreach { case (topic, categories) => {
        categories.toList.sortBy(-_._2).foreach { case (category, score) => writers(topic).println(category.getCategory+"\t"+1/score)}
      }}

      writers.foreach(_._2.close())
    }
    else
      topicCategories.foreach { case (topic,categories) => alreadyProcessed ++= categories.keySet }


    LOG.info(alreadyProcessed.size+" categories were already assigned")

    LOG.info("Assigning categories to topics utilizing topical classification")

    val topicCategoriesForTraining = Map[DBpediaTopic,Set[DBpediaCategory]]()
    scala.io.Source.fromFile(pathToTrainingCorpusCategories).getLines().foreach( category => {
      topicCategories.foreach { case (topic, categories)=>
        if (categories.keySet.contains(new DBpediaCategory(category))) {
          if (topicCategoriesForTraining.contains(topic))
            topicCategoriesForTraining(topic) += (new DBpediaCategory(category))
          else
            topicCategoriesForTraining += (topic -> Set(new DBpediaCategory(category)))
        }
      }
    })

    val nrOfExamples = topicCategoriesForTraining.foldLeft(Double.MaxValue)( (acc, topicCats) => math.min(acc,topicCats._2.size))

    topicCategories.foreach{ case (topic,cats) =>
      topicCategoriesForTraining.update(topic,
                Set[DBpediaCategory]() ++
                cats.filter(category => topicCategoriesForTraining(topic).contains(category._1)).toList.sortBy(-_._2).take(nrOfExamples.toInt).map(_._1).toSet
        ) }

    LOG.info("Writing new corpus for training topical classifier")
    //SplitOccsByTopics.splitOccs(pathToTempFlattenedHierarchy,pathToSortedArticlesCategories,pathToSortedOccs,pathToTempSplittedOccs)
    val tmpTopicsWriter = new PrintWriter(new FileWriter(tempTopicsPath))
    tmpTopicsWriter.println(topicCategories.keySet.toList.map(_.getTopic).sorted.reduceLeft( _+","+_ ))
    scala.io.Source.fromFile(pathToTrainingCorpusCategories).getLines().foreach( category => {
      var topicsString = ""
      topicCategoriesForTraining.foreach { case (topic, categories)=>
        if (categories.contains(new DBpediaCategory(category))) {
          topicsString += ","+topic.getTopic
        }
      }
      if (topicsString.isEmpty)
        topicsString = ",_ignore"
      tmpTopicsWriter.println(topicsString.substring(1))
    })

    tmpTopicsWriter.close()

    VowpalToArff.writeVowpalToArff(dictionary, pathToTrainingCorpus,tempTopicsPath,tempArffPath,normalize)

    LOG.info("Training model")
    NaiveBayesMultinomialClassifier.trainModel(tempArffPath, tempModelPath)

    LOG.info("Assign categories to topics with new model")
    val classifier = new NaiveBayesMultinomialClassifier(pathToDictionary, "", tempModelPath, dictionary.size, topicCategories.keySet.map(_.getTopic).toList)

    val lines = scala.io.Source.fromFile(pathToEvaluationCorpus).getLines()
    val topicLines = scala.io.Source.fromFile(pathToEvaluationCorpusCategories).getLines()

    var ctr = 0

    //do not allow years as categories, e.g. 1830_births, 1830_deaths -> too many of them - bring a lot of confusion, People_by People_from
    LOG.info("Categories starting with a year will not be assigned!")
    val pattern = Pattern.compile("((\\d\\d+)|(People_(from|by))).*|.*_people").matcher("")
    lines.foreach((fileLine) => {
      val category = new DBpediaCategory(topicLines.next())
      pattern.reset(category.getCategory)
      if (!alreadyProcessed.contains(category) && !pattern.matches()) {
        val catNameAsSet = Set() ++ vectorizer.getWordCountVector(category.getCategory.toLowerCase.replaceAll("[^a-z]", " ")).keySet
        val selectedTopics = Map[DBpediaTopic, Double]()

        topicKeywords.foreach { case (topic,keywords) => {
          var sum = 0.0
          keywords.foreach( keyword => sum += getScore(catNameAsSet,keyword) )

          if (sum >=1.0) {
            selectedTopics += (topic -> sum)
          }
        }}

        if (selectedTopics.size > 0) {
          var topics = selectedTopics.toList.sortBy(x => -x._2)
          topics = topics.takeWhile { case (topic, value) => value > 0.9*topics.head._2 }
          topics.foreach(topic => {
            topicCategories(topics.head._1) += (category -> topics.head._2)
          })
          ctr +=1
          if( ctr%100 == 0) {
            LOG.info("Assigned "+ctr+" new categories to topics")
            LOG.info("Latest assignment: " + category.getCategory + " -> " + topics.head._1.getTopic)
          }
        }
        else {
          var values =  List[(Int,Double)]()

          val split = fileLine.split(" ")

          if (split.length > 200) {
            var word : Array[String] = null
            var wordId = -1
            for(i <- 1 until split.length) {
              word = split(i).split(":")

              wordId = word(0).toInt
              if (wordId > -1)  {
                values = (wordId, word(1).toDouble) :: values
              }
            }
            //length normalization
            var squaredSum = 1.0
            if (normalize)
              squaredSum = math.sqrt(values.foldLeft(0.0)( (acc,element) => acc + element._2*element._2))

            values = values.sortBy(_._1)

            val result = classifier.getPredictions(values.map[Int,List[Int]](element => element._1).toArray, values.map[Double,List[Double]](element => element._2/squaredSum).toArray)

            result.foreach { case (topic, score) => {
              if (score>=classificationThreshold) {
                topicCategories(topic) += (category -> score)
                ctr +=1
                if( ctr%100 == 0) {
                  LOG.info("Assigned "+ctr+" new categories to topics")
                  LOG.info("Latest assignment: " + category.getCategory + " -> " + topic.getTopic)
                }
              }
            }}
          }
        }
      }
    })

    val writers = Map[DBpediaTopic,PrintWriter]()
    topicKeywords.foreach{ case (topic,keywords) => writers += (topic -> new PrintWriter(new FileWriter(output+"/"+topic.getTopic+".tsv"))) }

    topicCategories.foreach { case (topic, categories) => {
      categories.toList.sortBy(-_._2).foreach { case (category, score) => writers(topic).println(category.getCategory+"\t"+1/score)}
    }}

    writers.foreach(_._2.close())
  }

}
