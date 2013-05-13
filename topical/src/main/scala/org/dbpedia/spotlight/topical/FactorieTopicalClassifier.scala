package org.dbpedia.spotlight.topical

import cc.factorie._
import app.classify
import app.classify.ModelBasedClassifier
import java.io.{StringReader, File}
import io.Source
import optimize.{AROW, MIRA, SampleRankTrainer}
import org.dbpedia.spotlight.model.{Topic, Text}
import org.apache.commons.logging.LogFactory
import org.apache.lucene.analysis.{Analyzer}
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import scala.Predef._

/**
 * @author dirk
 * Date: 5/13/13
 * Time: 10:58 AM
 */

protected class FactorieTopicalClassifier extends TopicalClassifier {

    private val analyzer: Analyzer =  new EnglishAnalyzer(Version.LUCENE_36)

    class Document(text:String, labelName:String = "") extends FeatureVectorVariable[String] {
        def domain = documentDomain
        var label = new Label(labelName, this)

        {
            val tokenStream = analyzer.reusableTokenStream(null, new StringReader(text))
            val charTermAttribute = tokenStream.addAttribute(classOf[CharTermAttribute])
            while (tokenStream.incrementToken()) {
                if(charTermAttribute.toString().matches("""\w\w\w+"""))
                    this += charTermAttribute.toString()
            }
        }

    }
    class Label(name:String, val document:Document) extends LabeledCategoricalVariable(name) {
        def domain = labelDomain
    }
    protected var documentDomain = new CategoricalDimensionTensorDomain[String]{dimensionDomain.maxSize=200000}
    protected var labelDomain = new CategoricalDomain[String]{maxSize=50}

    protected var model = new CombinedModel(
            /** Bias term just on labels */
            new DotTemplateWithStatistics1[Label] {
                //override def statisticsDomains = Tuple1(LabelDomain)
                lazy val weights = new la.DenseTensor1(labelDomain.maxSize)
            },
            /** Factor between label and observed document */
            new DotTemplateWithStatistics2[Label,Document] {
                //override def statisticsDomains = ((LabelDomain, DocumentDomain))
                lazy val weights = new la.DenseTensor2(labelDomain.maxSize, documentDomain.dimensionDomain.maxSize)
                def unroll1 (label:Label) = Factor(label, label.document)
                def unroll2 (token:Document) = throw new Error("Document values shouldn't change")
            }
        )

    private val classifier = new ModelBasedClassifier[Label](model, labelDomain)
    /**
     * @param text
     * @return predicted probabilities of topics given the text
     */
    def getPredictions(text: Text) = {
        val doc = new Document(text.text)
        val classification = classifier.classify(doc.label)
        getTopics().zip(classification.proportions.asSeq).toArray
    }

    /**
     * @return list of topics the classifier is able to predict
     */
    def getTopics() = labelDomain.categories.map(cat => new Topic(cat)).toList

    private val objective = new HammingTemplate[Label]
    private lazy val trainer = new SampleRankTrainer(new GibbsSampler(model, objective), new AROW(model))
    /**
     * Trains the model on this text.
     * @param text
     * @param topic
     */
    def update(text: Text, topic: Topic) {
        val l = new Document(text.text,topic.getName).label
        val example = new optimize.DiscreteLikelihoodExample(l)
        trainer.processExamples(Seq(example))
    }

    def serialize(modelFile: File) {
        if (modelFile.getParentFile ne null)
            modelFile.getParentFile.mkdirs()
        BinaryFileSerializer.serialize(model, modelFile)
        val labelDomainFile = new File(modelFile.getAbsolutePath + "-labelDomain")
        BinaryFileSerializer.serialize(labelDomain, labelDomainFile)
        val featuresDomainFile = new File(modelFile.getAbsolutePath + "-documentDomain")
        BinaryFileSerializer.serialize(documentDomain.dimensionDomain, featuresDomainFile)
    }
}

object FactorieTopicalClassifier extends TopicalClassifierTrainer{
    private val LOG = LogFactory.getLog(getClass())

    var iterations = 1
    var batchSize = 100000

    def main(args:Array[String]) {
        trainModel(new File("/home/dirk/Downloads/20news-18828").listFiles().flatMap(topicDir => {
            topicDir.listFiles().map(file => {
                val source = Source.fromFile(file,"ISO-8859-1")
                val res = (new Topic(topicDir.getName), new Text(source.getLines().mkString(" ")))
                source.close()
                res
            })
        }).toIterator)
    }

    /**
     * @param corpus needs to be shuffled and of the following format: each line refers to a document with the following structure: topic\ttext
     */
    def trainModel(corpus:File):TopicalClassifier = {
        LOG.info("Training model on dataset " + corpus.getAbsolutePath)
        if (! corpus.exists) throw new IllegalArgumentException("Directory "+corpus+" does not exist.")

        trainModel(Source.fromFile(corpus).getLines().map(line => {
            val Array(topic,text) = line.split("\t",2)
            (new Topic(topic),new Text(text))
        }))
    }

    def trainModel(corpus:Iterator[(Topic,Text)]):TopicalClassifier = {
        val classifier = new FactorieTopicalClassifier()
        var documents = List[classifier.Document]()

        def doTrain {
            val examples = documents.shuffle.map(_.label)
            examples.foreach(_.setRandomly())
            (0 until iterations).foreach(i => {
                classifier.trainer.processContexts(examples)
                examples.foreach(_.setRandomly())
                val trainTrial = new classify.Trial[classifier.Label](classifier.classifier)
                trainTrial ++= examples
                println("Train accuracy = " + trainTrial.accuracy)
            })
        }

        corpus.foreach {
            case (topic,text) => {
                documents ::= new classifier.Document(text.text,topic.getName)
                if(documents.size >= batchSize) {
                    doTrain
                    documents = List[classifier.Document]()
                }
            }}

        if(documents.size < batchSize) {
            doTrain
        }

        classifier
    }

    def deSerialize(file:File):TopicalClassifier = {
        val classifier = new FactorieTopicalClassifier()
        val prefix = file.getAbsolutePath
        val labelDomainFile = new File(prefix + "-labelDomain")
        assert(labelDomainFile.exists(), "Trying to load inexistent label domain file: '" + prefix + "-labelDomain'")
        BinaryFileSerializer.deserialize(classifier.labelDomain, labelDomainFile)
        val featuresDomainFile = new File(prefix + "-documentDomain")
        assert(featuresDomainFile.exists(), "Trying to load inexistent label domain file: '" + prefix + "-featuresDomain'")
        BinaryFileSerializer.deserialize(classifier.documentDomain.dimensionDomain, featuresDomainFile)
        val modelFile = file
        assert(modelFile.exists(), "Trying to load inexisting model file: '" + prefix + "-model'")
        BinaryFileSerializer.deserialize(classifier.model, modelFile)
        classifier
    }
}
