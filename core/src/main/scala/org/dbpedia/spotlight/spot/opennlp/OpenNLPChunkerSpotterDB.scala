package org.dbpedia.spotlight.spot.opennlp


import org.dbpedia.spotlight.spot.Spotter
import java.util.LinkedList
import scala.util.control.Breaks._
import java.io.{InputStream, FileInputStream}
import org.dbpedia.spotlight.model.{Token, SurfaceForm, SurfaceFormOccurrence, Text}
import collection.mutable.ListBuffer
import opennlp.tools.chunker.{ChunkerModel, ChunkerME, Chunker}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import org.dbpedia.spotlight.db.model.SurfaceFormStore


/**
 * @author Joachim Daiber
 *
 * OpenNLP based Spotter performing NP chunking and selecting the longest sub-chunk in the dictionary of surface forms.
 *
 * This is similar to OpenNLPNGramSpotter but a bit simpler and uses a dictionary of known surface forms instead of NER.
 *
 */

class OpenNLPChunkerSpotterDB(
  chunkerModel: InputStream,
  surfaceFormStore: SurfaceFormStore,
  stopwords: Set[String],
  npTag: String = "NP",
  nnTag: String = "NN"
) extends Spotter {

  val MIN_ANNOTATION_PROBABILITY = 0.5

  val chunker: Chunker =
    new ChunkerME(new ChunkerModel(chunkerModel))

  def extract(text: Text): java.util.List[SurfaceFormOccurrence] = {
    val spots = new LinkedList[SurfaceFormOccurrence]
    val sentences: List[List[Token]] = tokensToSentences(text.feature("tokens").asInstanceOf[List[Token]])

    //Go through all sentences
    sentences.foreach{ sentence: List[Token] =>
      val tokens = sentence.map(_.token).toArray
      val tags = sentence.map(_.featureValue[String]("pos").get).toArray

      //Go through all chunks
      chunker.chunkAsSpans(tokens, tags)

        //Only look at NPs
        .filter(chunkSpan => chunkSpan.getType.equals(npTag))
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
                if ( !((lastToken == startToken) && !tags(startToken).toUpperCase.startsWith(nnTag) || stopwords.contains(spot.toLowerCase))) {
                  //The sub-chunk is in the dictionary, finish the processing of this chunk
                  spots.add(new SurfaceFormOccurrence(new SurfaceForm(spot), text, startOffset))
                  break()
                }
              }
              
            })
          }
      })
    }

    spots
  }

  private def surfaceFormMatch(spot: String): Boolean = {
    try {
      val sf = surfaceFormStore.getSurfaceForm(spot)
      sf.annotationProbability >= MIN_ANNOTATION_PROBABILITY
    } catch {
      case e: SurfaceFormNotFoundException => false
    }
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

}
