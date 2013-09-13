package org.dbpedia.spotlight.topical

import cc.factorie._
import app.classify.{Trial, LabelList, ModelBasedClassifier, LogLinearModel}
import java.io.{StringReader, File}
import io.Source
import la.DenseTensor1
import org.dbpedia.spotlight.model.{Topic, Text}
import org.dbpedia.spotlight.log.SpotlightLog
import org.apache.lucene.analysis.{Analyzer}
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import scala.Predef._
import collection.mutable.ArrayBuffer
import cc.factorie.util.{TensorCubbie, BinarySerializer}
import cc.factorie.app.classify

/**
 * @author dirk
 * Date: 5/13/13
 * Time: 10:58 AM
 */

/**
 * Complementary naive bayes as explained here (http://machinelearning.wustl.edu/mlpapers/paper_files/icml2003_RennieSTK03.pdf)
 */
protected class NaiveBayesTopicalClassifier extends TopicalClassifier {

    private val analyzer: Analyzer =  new EnglishAnalyzer(Version.LUCENE_36)

    class Document(text:String, labelName:String = labelDomain.category(0),training:Boolean = false) extends FeatureVectorVariable[String] {
        def domain = documentDomain
        final val docCountId = "###DOCCOUNT###"

        domain.dimensionDomain.gatherCounts = training
        //also count number of documents in document domain - this is hacky but works :)
        domain.dimensionDomain.index(docCountId)

        override def skipNonCategories = !training

        var label =
            if(training || labelDomain.categories.contains(labelName))
                new Label(labelName, this)
            else
                new Label(labelDomain.category(0), this)

        //I found that tf*idf and length normalization is very useful for high confidence scores (above 0.5 for one topic),
        //compared to just using unnormalized BOW vectors,
        //while overall prediction accuracy is just slightly improved over simple BOW
        {
            var group = Map[String,Double]()
            val tokenStream = analyzer.reusableTokenStream(null, new StringReader(text))
            val charTermAttribute = tokenStream.addAttribute(classOf[CharTermAttribute])
            while (tokenStream.incrementToken()) {
                try {
                    val token = charTermAttribute.toString().toLowerCase
                    if(token.toLowerCase.matches("[a-z]{3,}"))
                        group += (token -> (group.getOrElse(charTermAttribute.toString(),0.0)+1))
                } catch {
                    case e => //domain size exceeded max size. No problem just don't use that feature
                }
            }

            group.foreach(token => {
                this += (token._1,token._2)
            })
        }

        def normalize {
            var features = List[(Int,Double)]()
            this.tensor.foreachActiveElement{
                case (idx,tf) => {
                    try {
                        val df = domain.dimensionDomain.count(idx)
                        features ::= (idx, math.log(1+tf) * math.log( domain.dimensionDomain.count(docCountId) /  df))
                    } catch {
                        case e =>
                            idx;tf
                    }
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
    protected var documentDomain = new CategoricalTensorDomain[String]{ }
    protected var labelDomain = new CategoricalDomain[String]

    //labelIndex -> sum of all feature values for each label
    protected var totalMasses = new DenseTensor1(0)


    protected var _model:LogLinearModel[Label,Document] = null
    private var classifier = new ModelBasedClassifier[Label,LogLinearModel[Label,Document]](_model, labelDomain)
    /**
     * @param text
     * @return predicted probabilities of topics given the text
     */
    def getPredictions(text: Text) = {
        val doc = new Document(text.text)
        doc.normalize
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

        val tensorCubbie = new TensorCubbie[DenseTensor1]
        tensorCubbie.store(totalMasses)
        BinarySerializer.serialize(documentDomain.dimensionDomain,tensorCubbie,labelDomain,_model, modelFile,true)
    }

    //mainly copied from NaiveBayesTrainer; Complementary naive bayes as explained here (http://machinelearning.wustl.edu/mlpapers/paper_files/icml2003_RennieSTK03.pdf)
    private val biasSmoothingMass = 1.0
    private val evidenceSmoothingMass = 1.0
    private def trainIncremental(il: LabelList[Label, Document]) {
        il.foreach(_.document.normalize)
        //make model growable
        val cmodel =
            if(_model == null ||
               _model.evidenceTemplate.weights.value.dim2 < documentDomain.dimensionDomain.size ||
               _model.evidenceTemplate.weights.value.dim1 < labelDomain.size)
                new LogLinearModel[Label,Document](_.document, labelDomain, documentDomain)
            else
                _model

        val numLabels = labelDomain.size
        val numFeatures = documentDomain.dimensionDomain.size
        val bias = new DenseProportions1(numLabels)
        val cEvid = Seq.tabulate(numLabels)(i => new DenseProportions1(numFeatures))
        // Note: this doesn't actually build the graphical model, it just gathers smoothed counts, never creating factors
        // Incorporate smoothing, with simple +m smoothing

        for (li <- 0 until numLabels)
            bias.masses += (li, 1)//No biases, labelDomain.count(li)+biasSmoothingMass)

        val batchLabelCounts = Array.fill(numLabels){0.0}
        il.foreach(label => batchLabelCounts(label.intValue) += 1)

        if (_model == null) {
            for (li <- 0 until numLabels; fi <- 0 until numFeatures) cEvid(li).masses += (fi, evidenceSmoothingMass)
        } else {
            for (li <- 0 until numLabels ; fi <- 0 until numFeatures)
                if( _model.evidenceTemplate.weights.value.dim1 > li && _model.evidenceTemplate.weights.value.dim2 > fi)
                    //initialize with prior statistics. prior#docs * p(token|label)
                    cEvid(li).masses += (fi, totalMasses(li) * math.exp(-_model.evidenceTemplate.weights.value(li,fi)) ) //labelDomain does double counting
                else
                    cEvid(li).masses += (fi, evidenceSmoothingMass)
        }

        // Incorporate evidence
        for (label <- il) {
            val targetIndex = label.intValue
            val features = il.labelToFeatures(label)

            features.value.foreachActiveElement((featureIndex, featureValue) => {
                cEvid(targetIndex).masses += (featureIndex, featureValue)
            })
        }
        // Put results into the model templates
        totalMasses = new DenseTensor1(numLabels)
        (0 until numLabels).foreach(li => {
            totalMasses(li) = cEvid(li).masses.massTotal
            cmodel.biasTemplate.weights.value(li) = math.log(bias(li))

            for (fi <- 0 until numFeatures) {
                cmodel.evidenceTemplate.weights.value(li, fi) = math.log(cEvid(li)(fi))
            }
        })

        _model = cmodel
        classifier = new ModelBasedClassifier[Label,LogLinearModel[Label,Document]](_model, labelDomain)
    }
}

object NaiveBayesTopicalClassifier extends TopicalClassifierTrainer{
    var batchSize = 600000

    /**
     * @param corpus of following format: each line refers to a document with the following structure: topic\ttext
     */
    def trainModel(corpus:File, iterations:Int):TopicalClassifier = {
        val classifier = new NaiveBayesTopicalClassifier()
        trainModelIncremental(corpus,iterations,classifier)
        classifier
    }

    def trainModelIncremental(corpus: File, iterations: Int, classifier: TopicalClassifier) {
        SpotlightLog.info(this.getClass, "Training model on dataset %s", corpus.getAbsolutePath)

        if (! corpus.exists) throw new IllegalArgumentException("Directory "+corpus+" does not exist.")

        trainModelIncremental(Source.fromFile(corpus).getLines().map(line => {
            val Array(topic,text) = line.split("\t",2)
            (new Topic(topic),new Text(text))
        }), iterations,classifier)

        classifier
    }

    def trainModel(corpus:Iterator[(Topic,Text)],iterations:Int):TopicalClassifier = {
        val classifier = new NaiveBayesTopicalClassifier()
        trainModelIncremental(corpus,iterations,classifier)
        classifier
    }

    def trainModelIncremental(corpus: Iterator[(Topic, Text)], iterations: Int, classifier: TopicalClassifier) {
        val cl = classifier.asInstanceOf[NaiveBayesTopicalClassifier]
        cl.documentDomain.dimensionDomain.unfreeze()

        var documents = new ArrayBuffer[cl.Document]()
        var count = 0

        def doTrain {
            SpotlightLog.info(this.getClass, "Training on batch %d with %d documents", count, documents.size)
            count += 1
            val ll = new LabelList[cl.Label, cl.Document](documents.map(_.label), _.document)
            cl.trainIncremental(ll)
            /*
            val testTrial = new classify.Trial[cl.Label](cl.classifier)
            testTrial ++= ll

            println("acc="+testTrial.accuracy)

            def objective = new HammingTemplate[cl.Label]

            (0.1 until(1,0.1)).foreach(cutoff => {
                val cut = testTrial.filter(_.proportions.max >= cutoff)
                println("accuracy for cutoff: "+cutoff +" = "+cut.size.toDouble/testTrial.size+" - " +objective.accuracy(cut.map(_.label)))
            })*/
        }

        corpus.foreach {
            case (topic,text) => {
                documents += new cl.Document(text.text,topic.getName, true)
                if(documents.size >= batchSize) {
                    doTrain
                    documents.clear()
                }
            }}
        if(documents.size < batchSize) {
            doTrain
        }
        cl.documentDomain.dimensionDomain.freeze()
    }

    def deSerialize(file:File):TopicalClassifier = {
        val classifier = new NaiveBayesTopicalClassifier()

        classifier._model = new LogLinearModel[classifier.Label,classifier.Document](_.document, classifier.labelDomain,classifier.documentDomain)

        val tensorCubbie = new TensorCubbie[DenseTensor1]

        BinarySerializer.deserialize(classifier.documentDomain.dimensionDomain,tensorCubbie,classifier.labelDomain,classifier._model, file,true)

        classifier.totalMasses = tensorCubbie.fetch()

        classifier
    }
}
