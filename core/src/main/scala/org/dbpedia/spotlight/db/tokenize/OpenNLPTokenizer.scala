package org.dbpedia.spotlight.db.tokenize

import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.postag.POSTaggerME
import org.dbpedia.spotlight.model.{Feature, TokenType, Token, Text}
import opennlp.tools.util.Span
import org.dbpedia.spotlight.db.model.{TokenTypeStore, Stemmer}

/**
 * @author Joachim Daiber
 */

class OpenNLPTokenizer(
  tokenizer: opennlp.tools.tokenize.TokenizerME,
  stopWords: Set[String],
  stemmer: Stemmer,
  sentenceDetector: SentenceDetectorME,
  var posTagger: POSTaggerME,
  tokenTypeStore: TokenTypeStore
) extends BaseTextTokenizer(tokenTypeStore, stemmer) {

  def sentsProbs = _sentsProbs
  def tokensProbs = _tokensProbs
  def posTagsProbs = _posTagsProbs

  private var _sentsProbs: Array[Double] = null
  private var _tokensProbs: Array[Double] = null
  private var _posTagsProbs:Array[Double] = null

  def tokenize(text: Text): List[Token] = this.synchronized {
    val sents = sentenceDetector.sentPosDetect(text.text)
    this._sentsProbs = sentenceDetector.getSentenceProbabilities()
    var tokensProbsList = List[Array[Double]]()
    var posTagsProbsList = List[Array[Double]]()
    val res = sents.map{ sentencePos: Span =>
      val sentence = sentencePos.getCoveredText(text.text).toString()

      val sentenceTokenPos = tokenizer.tokenizePos(sentence)
      val sentenceTokens   = Span.spansToStrings(sentenceTokenPos, sentence)
      val posTags          = if(posTagger != null) posTagger.tag(sentenceTokens) else Array[String]()

      tokensProbsList +:= tokenizer.getTokenProbabilities()
      posTagsProbsList +:= posTagger.probs()
      (0 to sentenceTokens.size-1).map{ i: Int =>
        val token = if (stopWords contains sentenceTokens(i)) {
          new Token(sentenceTokens(i), sentencePos.getStart + sentenceTokenPos(i).getStart, TokenType.STOPWORD)
        } else {
          new Token(sentenceTokens(i), sentencePos.getStart + sentenceTokenPos(i).getStart, getStemmedTokenType(sentenceTokens(i)))
        }

        if(posTagger != null)
          token.setFeature(new Feature("pos", posTags(i)))

        if(i == sentenceTokens.size-1)
          token.setFeature(new Feature("end-of-sentence", true))

        token
      }
    }.flatten.toList
    this._tokensProbs = tokensProbsList.reverse.flatten.toArray
    this._posTagsProbs = posTagsProbsList.reverse.flatten.toArray
    res
  }

  def getStringTokenizer: BaseStringTokenizer = new OpenNLPStringTokenizer(tokenizer, stemmer)

}

class OpenNLPStringTokenizer(tokenizer: opennlp.tools.tokenize.Tokenizer, stemmer: Stemmer) extends BaseStringTokenizer(stemmer) {

  def tokenizeUnstemmed(text: String): Seq[String] = this.synchronized{ tokenizer.tokenize(text) }

  def tokenizePos(text: String): Array[Span] = this.synchronized{ tokenizer.tokenizePos(text) }

  override def setThreadSafe(isThreadSafe: Boolean) {

  }
}
