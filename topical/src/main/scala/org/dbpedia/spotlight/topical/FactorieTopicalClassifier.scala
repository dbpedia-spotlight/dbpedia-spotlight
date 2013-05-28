package org.dbpedia.spotlight.topical

import cc.factorie._
import app.classify
import classify.{LogLinearModel, Classifier, LabelList, ModelBasedClassifier}
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
import collection.mutable.ArrayBuffer

/**
 * @author dirk
 * Date: 5/13/13
 * Time: 10:58 AM
 */

protected class FactorieTopicalClassifier extends TopicalClassifier {

    private val analyzer: Analyzer =  new EnglishAnalyzer(Version.LUCENE_36)

    class Document(text:String, labelName:String = "") extends BinaryFeatureVectorVariable[String] {
        def domain = documentDomain
        var label = new Label(labelName, this)

        {
            val tokenStream = analyzer.reusableTokenStream(null, new StringReader(text))
            val charTermAttribute = tokenStream.addAttribute(classOf[CharTermAttribute])
            while (tokenStream.incrementToken()) {
                try {
                    this += charTermAttribute.toString()
                } catch {
                    case e => //domain size exceeded max size. No problem just don't use that feature
                }
            }
        }

    }
    class Label(name:String, val document:Document) extends LabeledCategoricalVariable(name) {
        def domain = labelDomain
    }
    protected var documentDomain = new CategoricalDimensionTensorDomain[String]{dimensionDomain.maxSize=1000000}
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
    private lazy val trainer = new SampleRankTrainer(new GibbsSampler(model, objective), new MIRA)
    /**
     * Trains the model on this text.
     * @param text
     * @param topic
     */
    def update(text: Text, topic: Topic) {
        val l = new Document(text.text,topic.getName).label
        trainer.processContext(l)
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

    var batchSize = 500000

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
    def trainModel(corpus:File, iterations:Int):TopicalClassifier = {
        LOG.info("Training model on dataset " + corpus.getAbsolutePath)
        if (! corpus.exists) throw new IllegalArgumentException("Directory "+corpus+" does not exist.")

        trainModel(Source.fromFile(corpus).getLines().map(line => {
            val Array(topic,text) = line.split("\t",2)
            (new Topic(topic),new Text(text))
        }), iterations)

    }

    def trainModel(corpus:Iterator[(Topic,Text)],iterations:Int):TopicalClassifier = {
        val classifier = new FactorieTopicalClassifier()
        var documents = new ArrayBuffer[classifier.Document]()

        def doTrain {
            val examples = documents.shuffle.map(_.label)
            examples.foreach(_.setRandomly())

            classifier.trainer.processContexts(examples)
            examples.foreach(_.setRandomly())
            val trainTrial = new classify.Trial[classifier.Label](classifier.classifier)
            trainTrial ++= examples
            LOG.info("Train accuracy = " + trainTrial.accuracy)
        }

        (0 until iterations).foreach(i => {
            corpus.foreach {
                case (topic,text) => {
                    documents += new classifier.Document(text.text,topic.getName)
                    if(documents.size >= batchSize) {
                        doTrain
                        documents.clear()
                    }
                }}
            if(documents.size < batchSize) {
                doTrain
            }

        })
        classifier
    }

    //mainly copied from factorie
    private var biasSmoothingMass = 1.0
    private var evidenceSmoothingMass = 1.0
    private def trainNaiveBayes[L <: LabeledMutableDiscreteVar[_], F <: DiscreteDimensionTensorVar](il: LabelList[L, F]): Classifier[L] = {
        val cmodel = new LogLinearModel(il.labelToFeatures, il.labelDomain, il.instanceDomain)(il.labelManifest, il.featureManifest)
        val labelDomain = il.labelDomain
        val featureDomain = il.featureDomain
        val numLabels = labelDomain.size
        val numFeatures = featureDomain.size
        val bias = new DenseProportions1(numLabels)
        val evid = Seq.tabulate(numLabels)(i => new DenseProportions1(numFeatures))
        // Note: this doesn't actually build the graphical model, it just gathers smoothed counts, never creating factors
        // Incorporate smoothing, with simple +m smoothing
        for (li <- 0 until numLabels) bias.masses += (li, biasSmoothingMass)
        for (li <- 0 until numLabels; fi <- 0 until numFeatures) evid(li).masses += (fi, evidenceSmoothingMass)
        // Incorporate evidence
        for (label <- il) {
            val targetIndex = label.intValue
            bias.masses += (targetIndex, 1.0)
            val features = il.labelToFeatures(label)
            val activeElements = features.tensor.activeElements
            while (activeElements.hasNext) {
                val (featureIndex, featureValue) = activeElements.next()
                evid(targetIndex).masses += (featureIndex, featureValue)
            }
        }
        // Put results into the model templates
        (0 until numLabels).foreach(i => cmodel.biasTemplate.weights(i) = math.log(bias(i)))
        for (li <- 0 until numLabels; fi <- 0 until numFeatures)
            cmodel.evidenceTemplate.weights(li * numFeatures + fi) = math.log(evid(li).apply(fi))
        new ModelBasedClassifier[L](cmodel, il.head.domain)
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
