package org.dbpedia.spotlight.db.tokenize

import org.tartarus.snowball.SnowballProgram
import org.dbpedia.spotlight.model.{Feature, TokenType, Token, Text}
import opennlp.tools.util.Span
import java.util.Locale
import java.text.BreakIterator
import collection.mutable.ArrayBuffer
import org.dbpedia.spotlight.db.model.{TokenTypeStore, Stemmer}


/**
 * @author Joachim Daiber
 */

class LanguageIndependentTokenizer(
  stopWords: Set[String],
  stemmer: Stemmer,
  locale: Locale,
  var tokenTypeStore: TokenTypeStore
) extends BaseAnnotationTokenizer(tokenTypeStore, stemmer) {

  def getRawTokenizer: BaseRawTokenizer = new LanguageIndependentRawTokenizer(locale, stemmer)

  def tokenize(text: Text): List[Token] = {

    Helper.tokenizeSentences(locale, text.text).map{ sentencePos: Span =>

      val sentence = text.text.substring(sentencePos.getStart, sentencePos.getEnd)

      val sentenceTokenPos = Helper.tokenizeWords(locale, sentence)
      val sentenceTokens   = sentenceTokenPos.map(s => sentence.substring(s.getStart, s.getEnd))

      (0 to sentenceTokens.size-1).map{ i: Int =>
        val token = if (stopWords contains sentenceTokens(i)) {
          new Token(sentenceTokens(i), sentencePos.getStart + sentenceTokenPos(i).getStart, TokenType.STOPWORD)
        } else {
          new Token(sentenceTokens(i), sentencePos.getStart + sentenceTokenPos(i).getStart, getStemmedTokenType(sentenceTokens(i)))
        }

        if(i == sentenceTokens.size-1)
          token.setFeature(new Feature("end-of-sentence", true))

        token
      }
    }.flatten.toList
  }
}

class LanguageIndependentRawTokenizer(locale: Locale, stemmer: Stemmer) extends BaseRawTokenizer(stemmer) {

  def tokenizeUnstemmed(text: String): Seq[String] = {
    Helper.tokenizeWords(locale, text).map{ s: Span =>
      text.substring(s.getStart, s.getEnd)
    }.toSeq
  }

  def tokenizePos(text: String): Array[Span] = Helper.tokenizeWords(locale, text)

}


object Helper {

  def tokenizeWords(locale: Locale, text: String): Array[Span] =
    tokenizeString(locale, BreakIterator.getWordInstance(locale), text)

  def tokenizeSentences(locale: Locale, text: String): Array[Span] =
    tokenizeString(locale, BreakIterator.getSentenceInstance(locale), text)

  def tokenizeString(locale: Locale, it: BreakIterator, text: String): Array[Span] = {
    it.setText(text)

    var spans = ArrayBuffer[Span]()

    var start = it.first()
    var end = it.next()

    while (end != BreakIterator.DONE) {
      if (!Character.isWhitespace(text.charAt(start)))
        spans += new Span(start, end)

      start = end
      end = it.next()
    }

    spans.toArray
  }

}

