package org.dbpedia.spotlight.db

import model.{TokenTypeStore, Tokenizer}
import org.tartarus.snowball.SnowballProgram
import org.dbpedia.spotlight.model.{Feature, TokenType, Token, Text}
import opennlp.tools.util.Span
import java.util.Locale
import java.text.BreakIterator
import collection.mutable.ArrayBuffer


/**
 * @author Joachim Daiber
 */

class LanguageIndependentTokenizer(
  stopWords: Set[String],
  stemmer: SnowballProgram,
  locale: Locale,
  var tokenTypeStore: TokenTypeStore
) extends Tokenizer {


  def tokenizeRaw(text: String): Seq[String] = {
    tokenizeWords(text).map{ s: Span =>
      getStemmedString( text.substring(s.getStart, s.getEnd) )
    }.toSeq
  }

  def tokenize(text: Text): List[Token] = {

    tokenizeSentences(text.text).map{ sentencePos: Span =>

      val sentence = text.text.substring(sentencePos.getStart, sentencePos.getEnd)

      val sentenceTokenPos = tokenizeWords(sentence)
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


  def getStemmedString(token: String): String = stemmer.synchronized {
    stemmer.setCurrent(token.toLowerCase)
    stemmer.stem()
    new String(stemmer.getCurrentBuffer)
  }

  def getStemmedTokenType(token: String): TokenType = tokenTypeStore.getTokenType(new String(getStemmedString(token)))


  private def tokenizeWords(text: String): Array[Span] =
    tokenizeString(BreakIterator.getWordInstance(locale), text)

  private def tokenizeSentences(text: String): Array[Span] =
    tokenizeString(BreakIterator.getSentenceInstance(locale), text)

  private def tokenizeString(it: BreakIterator, text: String): Array[Span] = {
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

