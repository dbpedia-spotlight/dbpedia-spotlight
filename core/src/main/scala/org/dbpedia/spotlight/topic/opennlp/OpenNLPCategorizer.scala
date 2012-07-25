package org.dbpedia.spotlight.topic.opennlp

import io.Source
import org.apache.commons.logging.LogFactory
import opennlp.tools.util.{ObjectStream, PlainTextByLineStream}
import java.io._
import weka.classifiers.evaluation.{NominalPrediction, ConfusionMatrix}
import opennlp.tools.doccat._

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 5/28/12
 * Time: 1:49 PM
 * To change this template use File | Settings | File Templates.
 */

object OpenNLPCategorizer {

  private val LOG = LogFactory.getLog(getClass())

  def main(args: Array[String]) {
    //trainModel("/media/Data/Wikipedia/model/training.corpus100k","/home/dirk/GSOC2012/20NewsGroup/en-doccat100k.bin")
    loadModel("/home/dirk/GSOC2012/20NewsGroup/en-doccat100k.bin")
    testModel("/media/Data/Wikipedia/model/test.corpus100k")
  }


  var model : DoccatModel = null

  def trainModel(corpusPath:String, outputPath:String) {
    var dataIn:InputStream = null
    try {
      dataIn = new FileInputStream(corpusPath)
      val lineStream = new PlainTextByLineStream(dataIn, "UTF-8")
      val sampleStream = new DocumentSampleStream(lineStream)

      model = DocumentCategorizerME.train("en", sampleStream)
    }
    finally {
      if (dataIn != null) {
        try {
          dataIn.close()
        }
      }
    }

    var modelOut: OutputStream = null
    try {
      modelOut = new BufferedOutputStream(new FileOutputStream(new File(outputPath)))
      model.serialize(modelOut)
    }
    finally {
      if (modelOut != null) {
        try {
          modelOut.close()
        }
      }
    }
  }

  def testModel(testCorpus:String) {
    val myCategorizer = new DocumentCategorizerME(model);
    val catArray = new Array[String](myCategorizer.getNumberOfCategories)
    for(i <- 0 until myCategorizer.getNumberOfCategories)
      catArray(i) = myCategorizer.getCategory(i)

    val matrix : ConfusionMatrix = new ConfusionMatrix(catArray)

    Source.fromFile(testCorpus).getLines().foreach( line => {
      val input = line.split("\t")
      val outcomes = myCategorizer.categorize(input(1));


      matrix.addPrediction(new NominalPrediction(
                                  myCategorizer.getIndex(input(0)),
                                  outcomes))
    })

    LOG.info( "\nError rate: "+matrix.errorRate()+
              "\nCorrect: "+matrix.correct()+
              "\nIncorrect: "+matrix.incorrect()+"\n\n"+
              matrix.toString("Confusion Matrix"))

  }

  def loadModel(modelPath:String) {
    // Deserialize from a file
    val file = new File(modelPath);
    val in = new FileInputStream(file);
    // Deserialize the object
    model = new DoccatModel(in)
  }

}
