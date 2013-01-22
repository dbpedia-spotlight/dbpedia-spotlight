package org.dbpedia.spotlight.db

import memory.MemoryStore
import model.{Tokenizer, SurfaceFormStore}
import org.dbpedia.spotlight.spot.opennlp.OpenNLPSpotter.spotFeatures
import breeze.linalg.{DenseMatrix, DenseVector}
import org.dbpedia.spotlight.io.AnnotatedTextSource
import breeze.regress.LinearRegression
import java.io.{FileInputStream, File}
import scala.io.Source
import org.dbpedia.spotlight.model.{SurfaceForm, SurfaceFormOccurrence, AnnotatedParagraph, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.io.WikipediaHeldoutCorpus
import org.apache.commons.io.FileUtils
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import org.dbpedia.spotlight.spot.opennlp.OpenNLPSpotter
import opennlp.tools.tokenize.{TokenizerModel, TokenizerME}
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetectorME}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import org.dbpedia.spotlight.spot.Spotter
import org.tartarus.snowball.ext.DutchStemmer
import scala.collection.JavaConversions._

object SpotterTuner {

  def tuneOpenNLP(corpus: AnnotatedTextSource, tokenizer: Tokenizer, spotter: OpenNLPSpotter, outputFile: File) {

    System.err.println("Tuning Spotter model...")

    val activeCorpus = corpus.take(20000)

    val allSpots = activeCorpus.map{ par: AnnotatedParagraph => {
      tokenizer.tokenizeMaybe(par.text)

      /* We are supposing the case that the tuning material is
         from Wikipedia. Therefore usually only the first link
         is annotated, but all links with the same name in the
         article should be annotated. For this reason, we use
         only the sf string to identify a spot.
      */
      (par.occurrences.map(_.surfaceForm.name).toSet, spotter.extract(par.text))
    }}


    val (nx, ny) = (spotFeatures(new SurfaceForm("test")).activeSize, allSpots.map(_._2.size()).sum)

    val x = DenseMatrix.zeros[Double](ny, nx)
    val y = DenseVector.zeros[Double](ny)

    var i = 0
    allSpots.foreach{
      case(goldSpotSet: Set[String], spots: java.util.List[SurfaceFormOccurrence]) => {
        spots.foreach{ spot: SurfaceFormOccurrence =>
          x(i,::) := spotFeatures(spot.surfaceForm).t
          y(i)     = ( if(goldSpotSet.contains(spot.surfaceForm.name)) 1.0 else 0.0 )
          i += 1
        }
      }

    }

    //System.err.println(x)

    FileUtils.write(
      outputFile,
      LinearRegression.regress(x, y).activeValuesIterator.mkString(" ")
    )

  }

}
