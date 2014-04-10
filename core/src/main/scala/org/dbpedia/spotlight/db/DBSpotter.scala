package org.dbpedia.spotlight.db

import model.{TextTokenizer, SurfaceFormStore}
import org.dbpedia.spotlight.spot.Spotter
import breeze.linalg.DenseVector
import org.dbpedia.spotlight.model._
import util.control.Breaks._
import scala.{None, Some}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import collection.mutable.ListBuffer
import opennlp.tools.util.Span
import opennlp.tools.namefind.RegexNameFinder
import java.util.regex.Pattern
import org.apache.commons.lang.StringUtils
import org.dbpedia.spotlight.log.SpotlightLog

abstract class DBSpotter(
 surfaceFormStore: SurfaceFormStore,
 spotFeatureWeights: Option[Seq[Double]],
 stopwords: Set[String]
) extends Spotter {

  var tokenizer: TextTokenizer = null

  val uppercaseFinder = new RegexNameFinder(
    Array[Pattern](
      Pattern.compile("([A-Z][^ ,!?.:;]*[ ]?)+")
    )
  )

  def findUppercaseSequences(tokens: Array[String]) = uppercaseFinder.find(tokens).map{ s: Span => new Span(s.getStart, s.getEnd, "Capital_Sequences") }.toArray

  val spotFeatureWeightVector: Option[DenseVector[Double]] = spotFeatureWeights match {
    case Some(w) => Some(DenseVector(w.toArray:_*))
    case None => None
  }

  def generateCandidates(sentence: List[Token]): Seq[Span]

  val MIN_CONFIDENCE = 0.1

  def extract(text: Text): java.util.List[SurfaceFormOccurrence] = {

    if (tokenizer != null)
      tokenizer.tokenizeMaybe(text)

    var spots = ListBuffer[SurfaceFormOccurrence]()
    val sentences: List[List[Token]] = DBSpotter.tokensToSentences(text.featureValue[List[Token]]("tokens").get)

    //Go through all sentences
    sentences.foreach{ sentence: List[Token] =>
      val spans = generateCandidates(sentence)

      val tokenTypes = sentence.map(_.tokenType).toArray

      spans.sorted
        .foreach(chunkSpan => {
        breakable {

          val firstToken = chunkSpan.getStart
          val lastToken = chunkSpan.getEnd-1

          val tokenSeqs = ListBuffer[(Int, Int)]()

          //Taking away a left member in each step, look for the longest sub-chunk in the SF dictionary
          (firstToken to lastToken).foreach{ startToken =>
            tokenSeqs += Pair(startToken, lastToken)
          }

          //Then, do the same in the other direction:
          (firstToken to lastToken).reverse.foreach{ endToken =>
            tokenSeqs += Pair(firstToken, endToken)
          }

          tokenSeqs.foreach{
            case (startToken: Int, endToken: Int) => {
              val startOffset = sentence(startToken).offset
              val endOffset = sentence(endToken).offset + sentence(endToken).token.length

              val spot = text.text.substring(startOffset, endOffset)

              //SpotlightLog.info(this.getClass, spot + ":" + chunkSpan.getType)

              val confidence = text.featureValue[Double]("confidence").getOrElse(0.5)
              val sfMatch = surfaceFormMatch(spot, confidence=math.max(MIN_CONFIDENCE, confidence))

              SpotlightLog.debug(this.getClass, "type:"+chunkSpan.getType)
              if (sfMatch.isDefined) {
                //The sub-chunk is in the dictionary, finish the processing of this chunk
                val spotOcc = new SurfaceFormOccurrence(sfMatch.get, text, startOffset, Provenance.Annotation, spotScore(spot)._2)
                spotOcc.setFeature(new Nominal("spot_type", chunkSpan.getType))
                spotOcc.setFeature(new Feature("token_types", tokenTypes.slice(startToken, lastToken)))
                spots += spotOcc
                break()
              }
            }
          }
        }
      })
    }

    dropOverlappingSpots(spots)
  }


  /**
   * This is the most important method in this class. Given the set of possible matches,
   * which are very general (e.g. based on stems in FSASpotter), we need to find a score
   * for each match. Matches will be filtered out by this score.
   *
   * @param spot
   * @return
   */
  private def spotScore(spot: String): (Option[SurfaceForm], Double) = {
    try {
      spotFeatureWeightVector match {
        case Some(weights) => {

          val (sf, p) = try {
            val sf = surfaceFormStore.getSurfaceForm(spot)
            (sf, sf.annotationProbability)
          } catch {
            case e: SurfaceFormNotFoundException => {
              surfaceFormStore.getRankedSurfaceFormCandidates(spot).headOption match {
                case Some(p) => p
                case None => throw e
              }
           }
          }

          sf.name = spot
          (Some(sf), weights dot DBSpotter.spotFeatures(spot, p))
        }
        case None => (Some(surfaceFormStore.getSurfaceForm(spot)), surfaceFormStore.getSurfaceForm(spot).annotationProbability)
      }
    } catch {
      case e: Exception => (None, 0.0)
    }
  }

  protected def surfaceFormMatch(spot: String, confidence: Double): Option[SurfaceForm] = {
    val score: (Option[SurfaceForm], Double) = spotScore(spot)
    score._1 match {
      case Some(sf) => SpotlightLog.debug(this.getClass, sf.toString + ":" + score._2)
      case None => SpotlightLog.debug(this.getClass, "None :" + score._2)
    }

    if (spotFeatureWeightVector.isDefined)
       if(score._2 >= confidence)
         score._1
       else
        None
    else
      if(score._2 >= 0.25)
        score._1
      else
        None

  }


  def typeOrder: Array[String]

  /**
   * This method resolves overlap conflicts in spots by considering their source (e.g. NER, chunking) and
   * their scores.
   *
   * @param spots
   * @return
   */
  def dropOverlappingSpots(spots: Seq[SurfaceFormOccurrence]): java.util.LinkedList[SurfaceFormOccurrence] = {

    val sortedSpots = spots.distinct.sortBy(sf => (sf.textOffset, sf.surfaceForm.name.length) )

    var remove = Set[Int]()
    var lastSpot: SurfaceFormOccurrence = null

    var i = 0
    while (i < sortedSpots.size) {

      val spot = sortedSpots(i)

      if (lastSpot != null && lastSpot.intersects(spot)) {

        val spotHasBetterType = typeOrder.indexOf(spot.featureValue[String]("spot_type")) < typeOrder.indexOf(lastSpot.featureValue[String]("spot_type"))
        val spotIsLonger = spot.surfaceForm.name.length > lastSpot.surfaceForm.name.length

        if(spotIsLonger && spot.spotProb > lastSpot.spotProb/2.0) {
          remove += i-1
          lastSpot = spot
        } else if(!spotIsLonger && !(spot.spotProb > lastSpot.spotProb*2.0)) {
          remove += i
          lastSpot = lastSpot
        } else if(spot.spotProb == lastSpot.spotProb && spotHasBetterType) {
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

object DBSpotter {
  def spotFeatures(spot: String, spotProbability: Double): DenseVector[Double] =
    DenseVector(
      //Annotation probability:
      spotProbability,

      //Abbreviations:
      if(spot.toUpperCase.equals(spot) && spot.size < 5 && !spot.matches("[0-9]+")) 1.0 else 0.0,

      //Numbers (e.g. years):
      if(spot.matches("[0-9]+")) 1.0 else 0.0,

      //Bias:
      1.0
    )

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
}
