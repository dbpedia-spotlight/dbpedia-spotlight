package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.model._
import opennlp.tools.chunker.{ChunkerModel, ChunkerME}
import org.dbpedia.spotlight.db.model.SurfaceFormStore
import opennlp.tools.namefind.{RegexNameFinder, TokenNameFinderModel, NameFinderME}
import opennlp.tools.util.Span
import java.util.regex.Pattern
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
) extends DBSpotter(surfaceFormStore, spotFeatureWeights, stopwords) {

  val chunker = chunkerModel match {
    case Some(m) => Some(new ChunkerME(m))
    case None => None
  }

  val ners = nerModels.map{ m: TokenNameFinderModel =>
    new NameFinderME(m)
  }

  def generateCandidates(sentence: List[Token]): Seq[Span] = {

    val tokens = sentence.map(_.token).toArray

    var spans = findUppercaseSequences(tokens)

    chunker match {
      case Some(c) => {
        val tags = sentence.map(_.featureValue[String]("pos").get).toArray
        this.synchronized {
          spans ++= c.chunkAsSpans(tokens, tags).filter(chunkSpan => phraseTags.contains(chunkSpan.getType))
        }
      }
      case None =>
    }

    if (!ners.isEmpty)
      this.synchronized {
        spans ++= ners.flatMap(_.find(tokens))
      }

    spans
  }

  def typeOrder = Array.concat(Array("person", "organization", "location", "misc"), phraseTags.toArray, Array("Capital_Sequences"))

  private var name = "Spotter based on an OpenNLP NP chunker and a simple spot dictionary."
  def getName = name
  def setName(name: String) {
    this.name = name
  }

}


