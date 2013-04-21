package org.dbpedia.spotlight.db.tokenize

import opennlp.tools.sentdetect.SentenceDetector
import opennlp.tools.postag.POSTagger
import org.dbpedia.spotlight.model.{Feature, TokenType, Token, Text}
import opennlp.tools.util.Span
import org.dbpedia.spotlight.db.model.{TokenTypeStore, Stemmer}

/**
 * @author Joachim Daiber
 */

class OpenNLPTokenizer(
  tokenizer: opennlp.tools.tokenize.Tokenizer,
  stopWords: Set[String],
  stemmer: Stemmer,
  sentenceDetector: SentenceDetector,
  var posTagger: POSTagger,
  tokenTypeStore: TokenTypeStore
) extends BaseTextTokenizer(tokenTypeStore, stemmer) {

  def tokenize(text: Text): List[Token] = this.synchronized {
    sentenceDetector.sentPosDetect(text.text).map{ sentencePos: Span =>

      val sentence = text.text.substring(sentencePos.getStart, sentencePos.getEnd)

      val sentenceTokens   = tokenizer.tokenize(sentence)
      val sentenceTokenPos = tokenizer.tokenizePos(sentence)
      val posTags          = if(posTagger != null) posTagger.tag(sentenceTokens) else Array[String]()

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
  }

  def getStringTokenizer: BaseStringTokenizer = new OpenNLPStringTokenizer(tokenizer, stemmer)

}

class OpenNLPStringTokenizer(tokenizer: opennlp.tools.tokenize.Tokenizer, stemmer: Stemmer) extends BaseStringTokenizer(stemmer) {

  def tokenizeUnstemmed(text: String): Seq[String] = this.synchronized{ tokenizer.tokenize(text) }

  def tokenizePos(text: String): Array[Span] = this.synchronized{ tokenizer.tokenizePos(text) }

}
