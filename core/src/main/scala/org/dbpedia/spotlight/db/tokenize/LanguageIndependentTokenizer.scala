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
) extends BaseTextTokenizer(tokenTypeStore, stemmer) {

  def getStringTokenizer: BaseStringTokenizer = new LanguageIndependentStringTokenizer(locale, stemmer)

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

class LanguageIndependentStringTokenizer(locale: Locale, stemmer: Stemmer) extends BaseStringTokenizer(stemmer) {

  def tokenizeUnstemmed(text: String): Seq[String] = {
    Helper.tokenizeWords(locale, text).map{ s: Span =>
      text.substring(s.getStart, s.getEnd)
    }.toSeq
  }

  def tokenizePos(text: String): Array[Span] = Helper.tokenizeWords(locale, text)

}


object Helper {

  val normalizations = Map[String, List[(String, String)]](
    "fr" -> List( ("([dDlL])[’']", "$1 ") ), //French def. and indef. article
    "it" -> List( ("([lL]|[uU]n)[’']", "$1 ") ), //Italian def. and indef. article
    "en" -> List( ("[’']s", " s") ) //normalize possesive
  )

  def normalize(locale: Locale, text: String): String = {
    var normalizedText = text

    normalizations.get(locale.getLanguage).getOrElse(List.empty).foreach{ n: Pair[String, String] =>
      normalizedText = normalizedText.replaceAll(n._1, n._2)
    }

    normalizedText
  }

  def tokenizeWords(locale: Locale, text: String): Array[Span] =
    tokenizeString(locale, BreakIterator.getWordInstance(locale), text)

  def tokenizeSentences(locale: Locale, text: String): Array[Span] =
    tokenizeString(locale, BreakIterator.getSentenceInstance(locale), text)


  def tokenizeString(locale: Locale, it: BreakIterator, text: String): Array[Span] = {
    val normalizedText = normalize(locale, text)
    it.setText( normalizedText )
    var spans = ArrayBuffer[Span]()

    var start = it.first()

    var end = try {
      it.next()
    } catch {
      case e: java.lang.ArrayIndexOutOfBoundsException =>
        System.err.println("Encountered JVM bug JDK-7104012, consider upgrading to Java 8!")
        it.setText( java.text.Normalizer.normalize(normalizedText, java.text.Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "") )
        it.next()
    }

    while (end != BreakIterator.DONE) {
      if ((start until end) exists (i => ! Character.isWhitespace(normalizedText.charAt(i))))
        spans += new Span(start, end)

      start = end
      end = it.next()
    }

    spans.toArray
  }

}

