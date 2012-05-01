package org.dbpedia.spotlight.spot.spotters

import opennlp.tools.chunker.{ChunkerModel, ChunkerME, Chunker}
import java.util.LinkedList
import scala.util.control.Breaks._
import collection.mutable.HashSet
import io.Source
import java.io.InputStream
import org.dbpedia.spotlight.model.{RequiresAnalyzedText, SurfaceForm, SurfaceFormOccurrence, Text}
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.spot.dictionary.SurfaceFormDictionary


/**
 * @author Joachim Daiber
 *
 * OpenNLP based Spotter performing NP chunking and selecting the longest sub-chunk in the dictionary of surface forms.
 *
 */

class OpenNLPChunkerSpotter(
  val chunkerModel: InputStream,
  val surfaceFormDictionary: SurfaceFormDictionary,
  val stopwordsFile: InputStream,
  val npTag: String,
  val nnTag: String
) extends Spotter with RequiresAnalyzedText {

  val chunker: Chunker =
    new ChunkerME(new ChunkerModel(chunkerModel))

  val stopwords = new HashSet[String]()
  Source.fromInputStream(stopwordsFile).getLines().foreach {
    line =>
      stopwords.add(line.trim())
  }


  def extract(text: Text): java.util.List[SurfaceFormOccurrence] = {
    val spots = new LinkedList[SurfaceFormOccurrence]
    val sentences = text.analysis.get.sentences

    //Go through all sentences
    var i = 0
    sentences.foreach(sentencePosition => {

      val sentence = text.text.substring(sentencePosition.getStart, sentencePosition.getEnd)
      val tokens = text.analysis.get.tokens(i)
      val tokensPositions = text.analysis.get.tokenPositons(i)
      val tags = text.analysis.get.posTags(i)

      //Go through all chunks
      chunker.chunkAsSpans(tokens, tags)

        //Only look at NPs
        .filter(chunkSpan => chunkSpan.getType.equals("NP"))
        .foreach(chunkSpan => {
        breakable {
          val firstToken = chunkSpan.getStart
          val lastToken = chunkSpan.getEnd - 1

          //Taking away a left member in each step, look for the longest sub-chunk in the SF dictionary
          (firstToken to lastToken).foreach(startToken => {
            val startOffset: Int = tokensPositions(startToken).getStart
            val endOffset: Int = tokensPositions(lastToken).getEnd
            val spot = sentence.substring(startOffset, endOffset)

            if (surfaceFormDictionary.contains(spot)) {

              if (!((lastToken == startToken) && !tags(startToken).toUpperCase.startsWith("NN") || stopwords.contains(spot.toLowerCase))) {
                //The sub-chunk is in the dictionary, finish the processing of this chunk
                spots.add(new SurfaceFormOccurrence(new SurfaceForm(spot), text, sentencePosition.getStart + startOffset))
                break()
              }
            }

          })
        }
      })
      i += 1
    })

    spots
  }


  private var name = "Spotter based on an OpenNLP NP chunker and a simple spot dictionary."


  def getName = name


  def setName(name: String) {
    this.name = name
  }
}
