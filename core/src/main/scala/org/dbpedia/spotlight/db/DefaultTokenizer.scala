package org.dbpedia.spotlight.db

import model.{TokenTypeStore, Tokenizer}
import org.tartarus.snowball.SnowballProgram
import opennlp.tools.sentdetect.SentenceDetector
import opennlp.tools.postag.POSTagger
import org.dbpedia.spotlight.model.{Feature, TokenType, Token, Text}
import opennlp.tools.util.Span

/**
 * @author Joachim Daiber
 */

class DefaultTokenizer(
  tokenizer: opennlp.tools.tokenize.Tokenizer,
  stopWords: Set[String],
  stemmer: SnowballProgram,
  sentenceDetector: SentenceDetector,
  var posTagger: POSTagger,
  tokenTypeStore: TokenTypeStore
) extends Tokenizer {

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


  def getStemmedTokenType(token: String): TokenType = {
    stemmer.setCurrent(token)
    stemmer.stem()
    val stemmed = stemmer.getCurrentBuffer
    tokenTypeStore.getTokenType(new String(stemmed))
  }
}
