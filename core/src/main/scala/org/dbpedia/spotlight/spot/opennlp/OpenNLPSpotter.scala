package org.dbpedia.spotlight.spot.opennlp


import org.dbpedia.spotlight.spot.Spotter
import scala.util.control.Breaks._
import java.io.InputStream
import org.dbpedia.spotlight.model._
import collection.mutable.ListBuffer
import opennlp.tools.chunker.{ChunkerModel, ChunkerME, Chunker}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import org.dbpedia.spotlight.db.model.SurfaceFormStore
import breeze.linalg.DenseVector
import opennlp.tools.namefind.{RegexNameFinder, TokenNameFinder, TokenNameFinderModel, NameFinderME}
import java.util
import opennlp.tools.util.Span
import util.regex.Pattern
import util.{Collections, Comparator}
import scala.Some


/**
 * @author Joachim Daiber
 *
 * OpenNLP based Spotter performing NP chunking and selecting the longest sub-chunk in the dictionary of surface forms.
 *
 * This is similar to OpenNLPNGramSpotter but a bit simpler and uses a dictionary of known surface forms.
 *
 */

class OpenNLPSpotter(
  chunkerModel: Option[ChunkerModel],
  nerModels: List[TokenNameFinderModel],
  surfaceFormStore: SurfaceFormStore,
  stopwords: Set[String],
  spotFeatureWeights: Option[Seq[Double]],
  phraseTags: Set[String] = Set("NP"),
  nnTag: String = "NN"
) extends Spotter {

  val spotFeatureWeightVector: Option[DenseVector[Double]] = spotFeatureWeights match {
    case Some(w) => Some(DenseVector(w.toArray:_*))
    case None => None
  }

  val chunker = chunkerModel match {
    case Some(m) => Some(new ChunkerME(m))
    case None => None
  }

  val ners = nerModels.map{ m: TokenNameFinderModel =>
    new NameFinderME(m)
  }

  val uppercaseFinder = new RegexNameFinder(
    Array[Pattern](
      Pattern.compile("([A-Z][^ ,!?.:;]*[ ]?)+")
    )
  )

  def extract(text: Text): java.util.List[SurfaceFormOccurrence] = {
    var spots = ListBuffer[SurfaceFormOccurrence]()
    val sentences: List[List[Token]] = tokensToSentences(text.featureValue[List[Token]]("tokens").get)

    //Go through all sentences
    sentences.foreach{ sentence: List[Token] =>
      val tokens = sentence.map(_.token).toArray
      val tokenTypes = sentence.map(_.tokenType).toArray

      //Go through all chunks
      var spans = uppercaseFinder.find(tokens).map{ s: Span => new Span(s.getStart, s.getEnd, "Capital_Sequences") }.toArray

      chunker match {
        case Some(c) => {
          val tags = sentence.map(_.featureValue[String]("pos").get).toArray
          spans ++= c.chunkAsSpans(tokens, tags).filter(chunkSpan => phraseTags.contains(chunkSpan.getType))
        }
        case None =>
      }

      if (!ners.isEmpty)
        spans ++= ners.flatMap(_.find(tokens))

      spans.sorted
        .foreach(chunkSpan => {
          breakable {

            val firstToken = chunkSpan.getStart
            val lastToken = chunkSpan.getEnd-1

            //Taking away a left member in each step, look for the longest sub-chunk in the SF dictionary
            (firstToken to lastToken).foreach(startToken => {
              val startOffset: Int = sentence(startToken).offset
              val endOffset: Int = sentence(lastToken).offset + sentence(lastToken).token.length
              val spot = text.text.substring(startOffset, endOffset)

              if (surfaceFormMatch(spot)) {
                  //The sub-chunk is in the dictionary, finish the processing of this chunk
                  val spotOcc = new SurfaceFormOccurrence(surfaceFormStore.getSurfaceForm(spot), text, startOffset, Provenance.Annotation, spotScore(spot))
                  spotOcc.setFeature(new Nominal("spot_type", chunkSpan.getType))
                  spotOcc.setFeature(new Feature("token_types", tokenTypes.slice(startToken, lastToken)))
                  spots += spotOcc
                  break()
                }
              
            })
          }
      })
    }

    dropOverlappingSpots(spots)
  }


  private var name = "Spotter based on an OpenNLP NP chunker and a simple spot dictionary."
  def getName = name
  def setName(name: String) {
    this.name = name
  }

  def tokensToSentences(allTokens: List[Token]): List[List[Token]] = {

    val sentences = ListBuffer[List[Token]]()
    val sentence = ListBuffer[Token]()

    allTokens foreach { token: Token =>
      sentence.append(token)

      token.feature("end-of-sentence") match {
        case Some(b) => {
          sentences.append(sentence.toList)
          sentence.clear()
        }
        case None =>
      }
    }

    sentences.toList
  }

  private def spotScore(spot: String): Double = {
    try {
      spotFeatureWeightVector match {
        case Some(weights) => {
          (weights dot OpenNLPSpotter.spotFeatures(surfaceFormStore.getSurfaceForm(spot)))
        }
        case None => surfaceFormStore.getSurfaceForm(spot).annotationProbability
      }
    } catch {
      case e: SurfaceFormNotFoundException => 0.0
      case _ => 0.0
    }
  }

  private def surfaceFormMatch(spot: String): Boolean = {
    spotScore(spot) > 0.45
  }


  val typeOrder = Array("person", "organization", "location", "misc") :+ phraseTags :+ Array("Capital_Sequences")

  /**
   * This method resolves overlap conflicts in spots by considering their source (e.g. NER, chunking) and
   * their scores.
   *
   * @param spots
   * @return
   */
  def dropOverlappingSpots(spots: Seq[SurfaceFormOccurrence]): java.util.LinkedList[SurfaceFormOccurrence] = {

    val sortedSpots = spots.sorted

    var remove = Set[Int]()
    var lastSpot: SurfaceFormOccurrence = null

    var i = 0
    while (i < sortedSpots.size) {

      val spot = sortedSpots(i)

      if (lastSpot != null && lastSpot.intersects(spot)) {

        val spotHasBetterType = typeOrder.indexOf(spot.featureValue[String]("spot_type")) < typeOrder.indexOf(lastSpot.featureValue[String]("spot_type"))

        if(spot.spotProb == lastSpot.spotProb && spotHasBetterType) {
          remove += i-1
          lastSpot = spot
        } else if (spot.spotProb == lastSpot.spotProb && !spotHasBetterType) {
          remove += i
          lastSpot = lastSpot
        } else if(spot.spotProb > lastSpot.spotProb) {
          remove += i-1
          lastSpot = spot
        } else {
          remove += i
          lastSpot = lastSpot
        }
      } else {
        lastSpot = spot
      }

      i += 1
    }

    //This is super inefficient :(
    val list = new java.util.LinkedList[SurfaceFormOccurrence]()
    sortedSpots.zipWithIndex.foreach{ case (s: SurfaceFormOccurrence, i: Int) =>
      if(!remove.contains(i))
        list.add(s)
    }
    list
  }

}

object OpenNLPSpotter {
  def spotFeatures(spot: SurfaceForm): DenseVector[Double] =
    DenseVector(
      //Annotation probability:
      spot.annotationProbability,

      //Abbreviations:
      if(spot.name.toUpperCase.equals(spot.name) && spot.name.size < 5 && !spot.name.matches("[0-9]+")) 1.0 else 0.0,

      //Bias:
      1.0
    )

}
