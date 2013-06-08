package org.dbpedia.spotlight.topical

import cc.factorie._
import app.classify.{Trial, LabelList, ModelBasedClassifier, LogLinearModel}
import java.io.{StringReader, File}
import io.Source
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

protected class NaiveBayesTopicalClassifier extends TopicalClassifier {

    private val analyzer: Analyzer =  new EnglishAnalyzer(Version.LUCENE_36)

    class Document(text:String, labelName:String = "",training:Boolean = false) extends FeatureVectorVariable[String] {
        def domain = documentDomain

        labelDomain.gatherCounts = training
        domain.dimensionDomain.gatherCounts = training

        domain.dimensionDomain.index(docCountId)

        var label = new Label(labelName, this)
        final val docCountId = "###DOCCOUNT###"


        {
            var group = Map[String,Double]()
            val tokenStream = analyzer.reusableTokenStream(null, new StringReader(text))
            val charTermAttribute = tokenStream.addAttribute(classOf[CharTermAttribute])
            while (tokenStream.incrementToken()) {
                try {
                    group += (charTermAttribute.toString() -> (group.getOrElse(charTermAttribute.toString(),0.0)+1))
                } catch {
                    case e => //domain size exceeded max size. No problem just don't use that feature
                }
            }

            group.foreach(token => this += (token._1,token._2))
        }

        def normalize {
            var features = List[(Int,Double)]()
            this.tensor.foreachActiveElement{
                case (idx,tf) => {
                    features ::= (idx, math.log(1+tf) * math.log( domain.dimensionDomain.count(docCountId) /  (domain.dimensionDomain.count(idx))))
                }
            }

            val length = math.sqrt(features.sumDoubles(entry => entry._2*entry._2))

            features.foreach{
                case (idx,ct) => this.tensor.update(idx, ct / length)
            }
        }

    }
    class Label(name:String, val document:Document) extends LabeledCategoricalVariable(name) {
        def domain = labelDomain
    }
    protected var documentDomain = new CategoricalDimensionTensorDomain[String]{}
    protected var labelDomain = new CategoricalDomain[String]

    protected var _model:LogLinearModel[Label,Document] = null
    private var classifier = new ModelBasedClassifier[Label](_model, labelDomain)
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

    /**
     * Trains the model on this text.
     * @param text
     * @param topic
     */
    def update(text: Text, topic: Topic) {
        val l = new Document(text.text,topic.getName).label
        val ll = new LabelList[Label, Document](List(l), _.document)
        trainIncremental(ll)
    }

    def serialize(modelFile: File) {
        if (modelFile.getParentFile ne null)
            modelFile.getParentFile.mkdirs()
        BinaryFileSerializer.serialize(_model, modelFile)
        val labelDomainFile = new File(modelFile.getAbsolutePath + "-labelDomain")
        BinaryFileSerializer.serialize(labelDomain, labelDomainFile)
        val featuresDomainFile = new File(modelFile.getAbsolutePath + "-documentDomain")
        BinaryFileSerializer.serialize(documentDomain.dimensionDomain, featuresDomainFile)
    }

    //mainly copied from NaiveBayesTrainer;
    // TODO: adding new labels that are not in the initial batch should be possible, but is because of some stupid bug (no idea which) not possible
    private val biasSmoothingMass = 1.0
    private val evidenceSmoothingMass = 1.0
    private def trainIncremental(il: LabelList[Label, Document]) {
        il.foreach(_.document.normalize)
        val cmodel =
            if(_model == null ||
               _model.evidenceTemplate.weights.dim2 < documentDomain.dimensionDomain.size ||
               _model.evidenceTemplate.weights.dim1 < labelDomain.size)
                new LogLinearModel[Label,Document](_.document, labelDomain, documentDomain)
            else
                _model

        val numLabels = labelDomain.size
        val numFeatures = documentDomain.dimensionDomain.size
        val bias = new DenseProportions1(numLabels)
        val evid = Seq.tabulate(numLabels)(i => new DenseProportions1(numFeatures))
        // Note: this doesn't actually build the graphical model, it just gathers smoothed counts, never creating factors
        // Incorporate smoothing, with simple +m smoothing

        for (li <- 0 until numLabels)
            bias.masses += (li, labelDomain.count(li)+biasSmoothingMass)

        val batchLabelCounts = Array.fill(numLabels){0.0}
        il.foreach(label => batchLabelCounts(label.intValue) += 1)

        if (_model == null) {
            for (li <- 0 until numLabels; fi <- 0 until numFeatures) evid(li).masses += (fi, evidenceSmoothingMass)
        } else {
            for (li <- 0 until numLabels ; fi <- 0 until numFeatures)
                if( _model.evidenceTemplate.weights.dim1 > li && _model.evidenceTemplate.weights.dim2 > fi)
                    evid(li).masses += (fi, (labelDomain.count(li)/2 - batchLabelCounts(li) ) * math.exp(_model.evidenceTemplate.weights(li,fi)) )
                else
                    evid(li).masses += (fi, evidenceSmoothingMass)
        }

        // Incorporate evidence
        for (label <- il) {
            val targetIndex = label.intValue
            val features = il.labelToFeatures(label)
            val activeElements = features.tensor.activeElements
            while (activeElements.hasNext) {
                val (featureIndex, featureValue) = activeElements.next()
                evid(targetIndex).masses += (featureIndex, featureValue)
            }
        }
        // Put results into the model templates
        (0 until numLabels).foreach(i =>
            cmodel.biasTemplate.weights(i) = math.log(bias(i)))

        for (li <- 0 until numLabels; fi <- 0 until numFeatures)
           cmodel.evidenceTemplate.weights(li, fi) = math.log(evid(li)(fi))


        _model = cmodel
        classifier = new ModelBasedClassifier[Label](_model, labelDomain)
    }
}

object NaiveBayesTopicalClassifier extends TopicalClassifierTrainer{
    private val LOG = LogFactory.getLog(getClass())

    var batchSize = 500000

    def main(args:Array[String]) {
        val m1 = trainModel(new File("/media/dirk/Data/Wikipedia/corpus.tsv")).asInstanceOf[NaiveBayesTopicalClassifier]
    }

    /**
     * @param corpus of following format: each line refers to a document with the following structure: topic\ttext
     */
    def trainModel(corpus:File, iterations:Int):TopicalClassifier = {
        LOG.info("Training model on dataset " + corpus.getAbsolutePath)
        if (! corpus.exists) throw new IllegalArgumentException("Directory "+corpus+" does not exist.")

        trainModel(Source.fromFile(corpus).getLines().map(line => {
            val Array(topic,text) = line.split("\t",2)
            (new Topic(topic),new Text(text))
        }), iterations).asInstanceOf[NaiveBayesTopicalClassifier]
    }

    def trainModel(corpus:Iterator[(Topic,Text)],iterations:Int):TopicalClassifier = {
        val classifier = new NaiveBayesTopicalClassifier()
        var documents = new ArrayBuffer[classifier.Document]()

        var count = 0

        def doTrain {
            LOG.info("Training on batch "+count+" with "+documents.size+" documents")
            count += 1
            val ll = new LabelList[classifier.Label, classifier.Document](documents.map(_.label), _.document)
            classifier.trainIncremental(ll)

            /*documents.foreach(_.label.setRandomly())
            val trainTrial = new Trial[classifier.Label](classifier.classifier)
            println(documents.size)
            trainTrial ++= documents.map(_.label)

            (0.1 until(1,0.1)).foreach(cutoff => {
                val cut = trainTrial.filter(_.proportions.max >= cutoff)
                println("accuracy for cutoff: "+cutoff +" = "+cut.size.toDouble/trainTrial.size+" - " +HammingObjective.accuracy(cut.map(_.label)))
            }) */
        }

        corpus.foreach {
            case (topic,text) => {
                documents += new classifier.Document(text.text,topic.getName, true)
                if(documents.size >= batchSize) {
                    doTrain
                    documents.clear()
                }
            }}
        if(documents.size < batchSize) {
            doTrain
        }

        classifier
    }

    def deSerialize(file:File):TopicalClassifier = {
        val classifier = new NaiveBayesTopicalClassifier()
        val prefix = file.getAbsolutePath
        val labelDomainFile = new File(prefix + "-labelDomain")
        assert(labelDomainFile.exists(), "Trying to load inexistent label domain file: '" + prefix + "-labelDomain'")
        BinaryFileSerializer.deserialize(classifier.labelDomain, labelDomainFile)
        val featuresDomainFile = new File(prefix + "-documentDomain")
        assert(featuresDomainFile.exists(), "Trying to load inexistent label domain file: '" + prefix + "-featuresDomain'")
        BinaryFileSerializer.deserialize(classifier.documentDomain.dimensionDomain, featuresDomainFile)
        val modelFile = file
        assert(modelFile.exists(), "Trying to load inexisting model file: '" + prefix + "-model'")
        BinaryFileSerializer.deserialize(classifier._model, modelFile)
        classifier
    }
}
