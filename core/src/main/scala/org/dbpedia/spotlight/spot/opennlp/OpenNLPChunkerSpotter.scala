package org.dbpedia.spotlight.spot.opennlp


import org.dbpedia.spotlight.spot.Spotter
import opennlp.tools.chunker.{ChunkerModel, ChunkerME, Chunker}
import java.io.FileInputStream
import java.io.File
import opennlp.tools.postag.{POSModel, POSTaggerME, POSTagger}
import opennlp.tools.tokenize.{TokenizerModel, TokenizerME, Tokenizer}
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetectorME, SentenceDetector}
import java.util.LinkedList
import scala.util.control.Breaks._
import collection.mutable.HashSet
import io.Source
import org.dbpedia.spotlight.model.{RequiresAnalyzedText, SurfaceForm, SurfaceFormOccurrence, Text}


/**
 * @author Joachim Daiber
 *
 * OpenNLP based Spotter performing NP chunking and selecting the longest sub-chunk in the dictionary of surface forms.
 *
 * This is similar to OpenNLPNGramSpotter but a bit simpler and uses a dictionary of known surface forms instead of NER.
 *
 */

class OpenNLPChunkerSpotter(
  chunkerModel: File,
  surfaceFormDictionary: SurfaceFormDictionary,
  stopwordsFile: File
) extends Spotter with RequiresAnalyzedText {

  val chunker: Chunker =
    new ChunkerME(new ChunkerModel(new FileInputStream(chunkerModel)))

  val stopwords = new HashSet[String]()
  Source.fromFile(stopwordsFile).getLines().foreach { line =>
    stopwords.add(line.trim())
  }

  def extract(text: Text): java.util.List[SurfaceFormOccurrence] = {
    val spots = new LinkedList[SurfaceFormOccurrence]
    val sentences = sentenceDetector.sentPosDetect(text.text)

    //Go through all sentences
    sentences.foreach(sentencePosition => {
      val sentence = text.text.substring(sentencePosition.getStart, sentencePosition.getEnd)

      val tokens = tokenizer.tokenize(sentence);
      val tokensPositions = tokenizer.tokenizePos(sentence);
      val tags = posTagger.tag(tokens)

      //Go through all chunks
      chunker.chunkAsSpans(tokens, tags)

        //Only look at NPs
        .filter(chunkSpan => chunkSpan.getType.equals("NP"))
        .foreach(chunkSpan => {
          breakable {
            val firstToken = chunkSpan.getStart
            val lastToken = chunkSpan.getEnd-1

            //Taking away a left member in each step, look for the longest sub-chunk in the SF dictionary
            (firstToken to lastToken).foreach(startToken => {
              val startOffset: Int = tokensPositions(startToken).getStart
              val endOffset: Int = tokensPositions(lastToken).getEnd
              val spot = sentence.substring(startOffset, endOffset)

              if (surfaceFormDictionary.contains(spot)) {

                if ( !((lastToken == startToken) && !tags(startToken).toUpperCase.startsWith("NN") || stopwords.contains(spot.toLowerCase))) {
                  //The sub-chunk is in the dictionary, finish the processing of this chunk
                  spots.add(new SurfaceFormOccurrence(new SurfaceForm(spot), text, sentencePosition.getStart + startOffset))
                  break()
                }
              }
              
            })
          }
      })
    })

    spots
  }

  private var name = "Spotter based on an OpenNLP NP chunker and a simple spot dictionary."
  def getName = name
  def setName(name: String) {
    this.name = name
  }
}
