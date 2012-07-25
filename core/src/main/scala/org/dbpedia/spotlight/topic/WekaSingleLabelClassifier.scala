package org.dbpedia.spotlight.topic

import utility.{TopicalStatInformation, TopicUtil, WordIdDictionary, TextVectorizerWithTransformation}
import weka.classifiers.bayes.NaiveBayesMultinomialUpdateable
import java.io._
import org.apache.commons.logging.{LogFactory, Log}
import java.util.{ArrayList,Properties}
import weka.core._
import scala.collection.mutable._
import org.dbpedia.spotlight.model.{Topic, Text}
import weka.core.converters.ArffLoader
import weka.classifiers.{UpdateableClassifier, Classifier}

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 5/30/12
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
object WekaSingleLabelClassifier{
  private val LOG = LogFactory.getLog(getClass())

  def fromProperties(properties:Properties):WekaSingleLabelClassifier =
    new WekaSingleLabelClassifier(TopicUtil.getDictionary(properties.getProperty("org.dbpedia.spotlight.topic.dictionary"),properties.getProperty("org.dbpedia.spotlight.topic.dictionary.maxsize").toInt),
                                        TopicUtil.getTopicInfo(properties.getProperty("org.dbpedia.spotlight.topic.categories.info")),
                                        new File(properties.getProperty("org.dbpedia.spotlight.topic.model.path")))


  def trainModel(arff:String, modelOut:String) {
    LOG.info("Training model on dataset "+arff+" and persist it at "+modelOut)
    // load data
    val loader:ArffLoader = new ArffLoader()
    loader.setFile(new File(arff))
    val structure = loader.getStructure
    structure.setClassIndex(structure.numAttributes() - 1)

    // train NaiveBayes
    val nb = new NaiveBayesMultinomialUpdateable()
    nb.buildClassifier(structure)
    var current:Instance = null
    while ({ current = loader.getNextInstance(structure); current != null})
      nb.updateClassifier(current)

    val oos = new ObjectOutputStream(
      new FileOutputStream(modelOut))
    oos.writeObject(nb)
    oos.flush()
    oos.close()
  }

  def trainModel(arff:String, structure:Instances, modelOut:String) {
    // load data
    val loader:ArffLoader = new ArffLoader()
    loader.setFile(new File(arff))

    structure.setClassIndex(structure.numAttributes() - 1)

    // train NaiveBayes
    val nb = new NaiveBayesMultinomialUpdateable()
    nb.buildClassifier(structure)
    var current:Instance = null
    while ({ current = loader.getNextInstance(structure); current != null})
      nb.updateClassifier(current)

    val oos = new ObjectOutputStream(
      new FileOutputStream(modelOut))
    oos.writeObject(nb)
    oos.flush()
    oos.close()
  }
}

class WekaSingleLabelClassifier(dictionary:WordIdDictionary,
                                val topicsInfo:TopicalStatInformation,
                                var modelFile:File,
                                var topics:List[Topic],
                                var transformInput:Boolean = true) extends TopicalClassifier {
  if (transformInput.equals(true))
    transformInput = (topicsInfo.getWordFrequencies(topicsInfo.getTopics.head).size > 0)

  def this(dictionary:WordIdDictionary, topicsInfo:TopicalStatInformation, modelFile:File) = this(dictionary, topicsInfo, modelFile,null)
  def this(dictionary:WordIdDictionary, modelFile:File, topics:List[Topic]) = this(dictionary, null, modelFile,topics)

  private val LOG:Log = LogFactory.getLog(getClass())

  private val translater = new TextVectorizerWithTransformation(dictionary,topicsInfo)

  if(topics==null)
    topics = topicsInfo.getTopics.toList.sortBy(_.getName)
  else
    topics = topics.sortBy(_.getName)


  // Declare the feature vector
  private val attributes = new ArrayList[Attribute](dictionary.getMaxSize+1)

  for (i <- 0 until dictionary.getMaxSize) {
    attributes.add(new Attribute("word_"+i))
  }

  attributes.add(new Attribute("class", scala.collection.JavaConversions.seqAsJavaList(topics.map[String,List[String]](_.getName))))

  private val dataset = new Instances("topics",attributes,0)
  dataset.setClassIndex(dataset.numAttributes()-1)

  private val classifier = {
    var result:Classifier = null
    if (modelFile.exists()) {
      try {
        result = new ObjectInputStream(
          new FileInputStream(modelFile)).
          readObject().asInstanceOf[Classifier]
        LOG.info("Loaded model from "+modelFile.getAbsolutePath)
      }
      catch {
        case e:ClassCastException => LOG.error("Specified model file: "+modelFile.getAbsolutePath+" is no valid weka classifier!")
      }
    }
    else {
      result = new NaiveBayesMultinomialUpdateable()
      result.buildClassifier(dataset)
      LOG.info("Built new model at "+modelFile.getAbsolutePath)
    }
    result
  }

  def getPredictions(text:Text):Array[(Topic,Double)] = {
    val vector = translater.textToTranformedInput(text.text,transformInput)
    getPredictions(vector.keys.toArray[Int], vector.values.toArray[Double])
  }

  def getPredictions(ids: Array[Int],values:Array[Double]) :Array[(Topic,Double)] = {
    val instance = new SparseInstance(1,values, ids,dictionary.getMaxSize)
    instance.setDataset(dataset)

    val predictions = classifier.distributionForInstance(instance)
    val result = new Array[(Topic,Double)](predictions.length)
    for (i <- 0 until predictions.length)
      result(i) = (getTopic(i),predictions(i))

    result
  }

  def getTopics():List[Topic] = topics

  private def getTopic(index:Int):Topic = topics(index)

  def update(text:Text, topic: Topic, increaseVocabulary: Int = 0) {
    val vector = translater.textToTranformedInput(text.text,transformInput,true, topic, increaseVocabulary)
    update(vector, topic)
  }

  def update(vector:Map[Int,Double], topic: Topic) {
     if (getTopics.contains(topic)) {
       val instance = new SparseInstance(1, vector.values.toArray[Double],vector.keys.toArray[Int],dictionary.getMaxSize)
       instance.setDataset(dataset)

       instance.setClassValue(topic.getName)

       classifier.asInstanceOf[UpdateableClassifier].updateClassifier(instance)
     }
  }

  def persistModel {
    val oos = new ObjectOutputStream(
      new FileOutputStream(modelFile))
    oos.writeObject(classifier)
    oos.flush()
    oos.close()
  }

  def persist {
    persistModel
    dictionary.persist
    topicsInfo.persist
  }

}
