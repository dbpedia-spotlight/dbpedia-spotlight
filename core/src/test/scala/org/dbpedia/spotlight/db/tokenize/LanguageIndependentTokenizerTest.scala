package org.dbpedia.spotlight.db.tokenize

import java.util.Locale


import org.junit.Test
import org.junit.Assert.assertTrue
import org.scalatest.junit.AssertionsForJUnit

/**
 * A class for testing the LanguageIndependentTokenizer
 * @author Thiago Galery (thiago.galery@idioplatform.com)
 * @author David Przybilla (david.przybilla@idioplatform.com)
 */

class LanguageIndependentTokenizerTest extends AssertionsForJUnit {

  @Test
  def tokenizeEnglishSentencesTest() {
    // Mock the locale used for tokenization.
    val enLocale = new Locale("en", "US")
    // Case 0: Input text is a sequence of blank spaces.
    val text0 = "     "
    val sentences0 = Helper.tokenizeSentences(enLocale, text0)
    // We expect no sentence to be extracted.
    assertTrue(sentences0.size.equals(0))

    // Case 1: Input text begins with whitespace.
    val text1 = " Barack  Obama lives in Washington, D.C. . This is another Sentence."
    val sentences1 = Helper.tokenizeSentences(enLocale, text1)
    //  We expect 2 sentences to be extracted.
    assertTrue(sentences1.size.equals(2))
    // Assertions about sentence tokenization.
    assertTrue(text1.subSequence(
      sentences1(0).getStart, sentences1(0).getEnd
      ).equals(" Barack  Obama lives in Washington, D.C. . ")
    )
    assertTrue(text1.subSequence(
      sentences1(1).getStart, sentences1(1).getEnd
      ).equals("This is another Sentence.")
    )

    val spans1 = Helper.tokenizeWords(enLocale,
      text1.subSequence(sentences1(0).getStart, sentences1(0).getEnd).toString
    )
    // Word Tokenization
    val tokens1 = spans1.map(s => text1.subSequence(s.getStart, s.getEnd).toString)
    val expectedTokens1 = List[String]("Barack", "Obama", "lives", "in", "Washington", "D.C", ".")
    // Assertions about word tokens
    for (expectedToken <- expectedTokens1) {
      assertTrue(tokens1.contains(expectedToken))
    }


    // Case 2: Input text doesn't contain trailing whitespaces or punctuation.
    val text2 = "Sentence without any trailing spaces or punctuation"
    val sentences2 = Helper.tokenizeSentences(enLocale, text2)
    //  We expect 1 sentences to be extracted.
    assertTrue(sentences2.size.equals(1))
    // Assertions about sentence tokenization.
    assertTrue(text2.subSequence(
      sentences2(0).getStart, sentences2(0).getEnd
      ).equals("Sentence without any trailing spaces or punctuation")
    )

    // Case 3: Input text contain lots of whitespace across sentence boundaries.
    val text3 = " This is sentence Uno        .     This is two.  That is sentence drei. "
    val sentences3 = Helper.tokenizeSentences(enLocale, text3)
    //  We expect 3 sentences to be extracted.
    assertTrue(sentences3.size.equals(3))
    // Assertions about sentence tokenization.
    assertTrue(text3.subSequence(
      sentences3(0).getStart, sentences3(0).getEnd
    ).equals(" This is sentence Uno        .     ")
    )
    assertTrue(text3.subSequence(
      sentences3(1).getStart, sentences3(1).getEnd
      ).equals("This is two.  ")
    )
    assertTrue(text3.subSequence(
      sentences3(2).getStart, sentences3(2).getEnd
      ).equals("That is sentence drei. ")
    )
  }

  @Test
  def tokenizeEnglishWordsTest() {
    // Mock the locale used for tokenization.
    val enLocale = new Locale("en", "US")
    // Case 0: Mock a sentence containing english contractions.
    val sentence0 = "India's elections might determine economic growth."
    val words0 = Helper.tokenizeWords(enLocale, sentence0)
    // Assert
    assertTrue(sentence0.subSequence(words0(0).getStart, words0(0).getEnd) == "India")

    //Mock the french locale for tokenization.
    val frLocale = new Locale("fr", "FR")
    // Case 1: Mock a sentence containing a french contraction.
    val sentence1 = "L'amour d’homme est tragique."
    val words1 = Helper.tokenizeWords(frLocale, sentence1)
    // Assert
    assertTrue(sentence1.subSequence(words1(1).getStart, words1(1).getEnd) == "amour")
    assertTrue(sentence1.subSequence(words1(3).getStart, words1(3).getEnd) == "homme")


    //Mock the french locale for tokenization.
    val itLocale = new Locale("it", "IT")
    // Case 2: Mock a sentence containing an italian contraction.
    val sentence2 = "Gianni fare l'università."
    val words2 = Helper.tokenizeWords(itLocale, sentence2)
    // Assert
    assertTrue(sentence2.subSequence(words2(3).getStart, words2(3).getEnd) == "università")

  }

}
