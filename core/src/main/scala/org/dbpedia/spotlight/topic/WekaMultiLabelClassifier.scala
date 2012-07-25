package org.dbpedia.spotlight.topic

import utility.{TextVectorizerWithTransformation, TopicalStatInformation, TopicUtil, WordIdDictionary}
import org.dbpedia.spotlight.model.{Text, Topic}
import java.io.{FilenameFilter, FileOutputStream, ObjectOutputStream, File}
import scala.collection.mutable._
import scala.Array
import weka.core.converters.ArffLoader
import weka.classifiers.bayes.{NaiveBayesUpdateable, NaiveBayesMultinomialUpdateable}
import weka.core.{Attribute, FastVector, Instances, Instance}
import weka.filters.unsupervised.attribute.{MakeIndicator, MergeManyValues}
import java.util.Properties
import weka.filters.Filter
import org.apache.commons.logging.LogFactory

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 6/27/12
 * Time: 5:53 PM
 * To change this template use File | Settings | File Templates.
 */
object WekaMultiLabelClassifier{

  private val LOG = LogFactory.getLog(getClass)

  /**
   *
   * @param args path to training corpus in arff (class labels have to be last attribute), path to output directory
   */
  def main(args:Array[String]) {
    trainModel(args(0),args(1))
  }

  def fromProperties(properties:Properties):WekaMultiLabelClassifier =
    new WekaMultiLabelClassifier(TopicUtil.getDictionary(properties.getProperty("org.dbpedia.spotlight.topic.dictionary"),properties.getProperty("org.dbpedia.spotlight.topic.dictionary.maxsize").toInt),
                                       TopicUtil.getTopicInfo(properties.getProperty("org.dbpedia.spotlight.topic.categories.info")),
                                       new File(properties.getProperty("org.dbpedia.spotlight.topic.model.path")), null)


  def trainModel(arff:String, modelOut:String) {
    new File(modelOut).mkdirs()

    // load data
    val loader:ArffLoader = new ArffLoader()
    loader.setFile(new File(arff))
    val structure = loader.getStructure

    var topicLabels = List[String]()
    val enumeration = structure.attribute(structure.numAttributes()-1).enumerateValues()

    while(enumeration.hasMoreElements)
      topicLabels ::= enumeration.nextElement().asInstanceOf[String]
    topicLabels = topicLabels.reverse

    structure.setClassIndex(structure.numAttributes()-1)

    val topicNumber = topicLabels.length
    LOG.info("Labels found: "+topicLabels.reduceLeft(_+", "+_))

    val threshold = 1.0-1.0/(topicLabels.size-1)
    val random = new util.Random()

    for(i <- 0 until topicNumber) {
      loader.reset()
      loader.setFile(new File(arff))
      loader.getStructure

      var valueRange = ""
      for (j <- 0 until topicNumber)
        if (j!=i)
          valueRange += ","+j
      valueRange = valueRange.substring(1)

      val classFilter = new MakeIndicator()
      classFilter.setAttributeIndex(""+(structure.classIndex()+1))
      classFilter.setValueIndex(i)
      classFilter.setNumeric(false)
      classFilter.setInputFormat(structure)

      val tempStructure = Filter.useFilter(structure, classFilter)

      // train NaiveBayes
      val nb = new NaiveBayesMultinomialUpdateable()
      nb.buildClassifier(tempStructure)
      var current:Instance = null
      var counter = 0
      val label =topicLabels(i)
      while ({ current = loader.getNextInstance(structure); current != null}) {
        if (current.stringValue(structure.classIndex())==label || threshold <= random.nextDouble()) {
          classFilter.input(current)
          nb.updateClassifier(classFilter.output())
          counter += 1
          if (counter%10000==0)
            LOG.info(counter + " examples processed for label "+label)
        }
      }

      LOG.info("Writing model for "+label+" to "+ modelOut+"/"+label+".model")
      val oos = new ObjectOutputStream(
        new FileOutputStream(modelOut+"/"+label+".model"))
      oos.writeObject(nb)
      oos.flush()
      oos.close()
    }
  }
}

class WekaMultiLabelClassifier(dictionary:WordIdDictionary,
                               topicsInfo:TopicalStatInformation,
                               modelsDir:File,
                               var topics:List[Topic],
                               var transformInput:Boolean = true) extends TopicalClassifier {

  if (transformInput.equals(true))
    transformInput = (topicsInfo.getWordFrequencies(topicsInfo.getTopics.head).size > 0)

  def this(dictionary:WordIdDictionary,  topicsInfo:TopicalStatInformation, modelFile:File) =  this(dictionary,topicsInfo,modelFile,null)

  private val LOG = LogFactory.getLog(getClass)

  val NEGATIVE_TOPIC_PREFIX="_"

  private val translater = new TextVectorizerWithTransformation(dictionary,topicsInfo)

  var models = Map[Topic,WekaSingleLabelClassifier]()

  {
    if(modelsDir.exists())
      modelsDir.listFiles(new FilenameFilter {
        def accept(file: File, name: String): Boolean = name.endsWith(".model")
      }).foreach( modelFile => {
        val name = modelFile.getName.replace(".model","")
        models += (new Topic(name) ->
          new WekaSingleLabelClassifier(dictionary, topicsInfo, modelFile, List(new Topic(name), new Topic(NEGATIVE_TOPIC_PREFIX+name))))
      } )
    else
      modelsDir.mkdirs()

    topicsInfo.getTopics.filterNot( topic => topic.equals(TopicUtil.OVERALL_TOPIC) || models.contains(topic)).foreach( topic => {
      models += (topic ->
        new WekaSingleLabelClassifier(dictionary, topicsInfo, new File(modelsDir,topic.getName+".model"), List(topic, new Topic(NEGATIVE_TOPIC_PREFIX+topic.getName))))
    })
  }

  def getPredictions(text:Text):Array[(Topic,Double)] = {
    var predictions = List[(Topic,Double)]()
    models.foreach { case(topic, classifier) => {
      predictions = (classifier.getPredictions(text).find( _._1.equals(topic)).get) :: predictions
    }}
    predictions.toArray
  }

  def getPredictions(ids: Array[Int],values:Array[Double]) : Array[(Topic,Double)] = {
    var predictions = List[(Topic,Double)]()
    models.foreach { case(topic, classifier) => {
      predictions = (classifier.getPredictions(ids, values).find( _._1.equals(topic)).get) :: predictions
    }}
    predictions.toArray
  }

  def getTopics():List[Topic] = models.keySet.toList.sortBy(_.getName)

  def update(text:Text, topic: Topic, increaseVocabulary: Int = 0) {
    val vector = translater.textToTranformedInput(text.text, transformInput, true, topic, increaseVocabulary)
    update(vector, topic)
  }

  def update(vector:Map[Int,Double], topic: Topic) {
    if (getTopics.contains(topic)) {
      models.foreach { case (modelTopic,model) => {
        if (modelTopic.equals(topic)) {
          model.update(vector, topic)
        }
        else if (math.random < 1.0/(models.size-1.0)) {
          model.update(vector, new Topic(NEGATIVE_TOPIC_PREFIX+modelTopic.getName))
        }
      }}
    }
  }

  def persist {
    models.foreach {
      case(topic, model) => {
        model.persistModel
      }
    }

    dictionary.persist
    topicsInfo.persist
  }
}
