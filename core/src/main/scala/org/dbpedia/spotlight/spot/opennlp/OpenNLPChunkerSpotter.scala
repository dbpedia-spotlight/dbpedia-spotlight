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
import org.dbpedia.spotlight.model.{SurfaceForm, SurfaceFormOccurrence, Text}
import collection.mutable.HashSet
import io.Source._


/**
 * @author Joachim Daiber
 *
 * OpenNLP based Spotter performing NP chunking and selecting the longest sub-chunk in the dictionary of surface forms.
 *
 * This is similar to OpenNLPNGramSpotter but a bit simpler and uses a dictionary of known surface forms instead of NER.
 *
 */

class OpenNLPChunkerSpotter(sentenceModel: File, tokenizerModel: File, posModel: File, chunkerModel: File, surfaceFormDictionaryFile: File) extends Spotter {

  val posTagger: POSTagger =
    new POSTaggerME(new POSModel(new FileInputStream(posModel)))

  val sentenceDetector: SentenceDetector =
    new SentenceDetectorME(new SentenceModel(new FileInputStream(sentenceModel)))

  val tokenizer: Tokenizer =
    new TokenizerME(new TokenizerModel(new FileInputStream(tokenizerModel)))

  val chunker: Chunker =
    new ChunkerME(new ChunkerModel(new FileInputStream(chunkerModel)))

  val surfaceFormDictionary: SurfaceFormDictionary = new SurfaceFormDictionaryCaseInsensitive(surfaceFormDictionaryFile)

  def extract(text: Text): java.util.List[SurfaceFormOccurrence] = {
    val spots = new LinkedList[SurfaceFormOccurrence]
    val sentences = sentenceDetector.sentDetect(text.text)

    //Go through all sentences
    sentences.foreach(sentence => {
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

              if(surfaceFormDictionary.contains(spot)) {
                //The sub-chunk is in the dictionary, finish the processing of this chunk
                spots.add(new SurfaceFormOccurrence(new SurfaceForm(spot), text, startOffset))
                break()
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

class SurfaceFormDictionary(dictionaryFile: File) {

  val lines = fromFile(dictionaryFile).getLines()
  val surfaceFormDictionary = new HashSet[String]
  lines.foreach(line => surfaceFormDictionary += normalizeEntry(line))

  def contains(surfaceform: String): Boolean = surfaceFormDictionary.contains(normalizeEntry(surfaceform))

  def normalizeEntry(entry: String) = entry
}

class SurfaceFormDictionaryCaseInsensitive(dictionaryFile: File) extends SurfaceFormDictionary(dictionaryFile) {
  override def normalizeEntry(entry: String) = entry.toLowerCase
}

object OpenNLPChunkerSpotter {
  def main(args: Array[String]) {
    val text: Text = new Text(" Col. Muammar el-Qaddafi, the former Libyan strongman who fled into hiding after an armed uprising toppled his regime two months ago, met a violent and vengeful death Thursday in the hands of rebel fighters who stormed his final stronghold in his Mediterranean hometown Surt. At least one of his sons was also killed.")
    val d = new File("/Users/jodaiber/Desktop/")
    val spotter: OpenNLPChunkerSpotter = new OpenNLPChunkerSpotter(new File(d, "en-sent.bin"), new File(d, "en-token.bin"), new File(d, "en-pos-maxent.bin"), new File(d, "en-chunker.bin"), new File(d, "spots.list"))
    val spots = spotter.extract(text)
    print()
  }
  

}