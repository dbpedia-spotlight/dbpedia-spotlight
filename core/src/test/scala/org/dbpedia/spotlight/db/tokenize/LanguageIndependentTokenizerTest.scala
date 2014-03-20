package org.dbpedia.spotlight.db.tokenize

import java.util.Locale


import org.junit.Test
import org.junit.Assert.assertTrue
import org.scalatest.junit.AssertionsForJUnit

/**
 * A class for testing the LanguageIndependentTokenizer
 * @author tgalery
 * @author dav009
 */

class LanguageIndependentTokenizerTest extends AssertionsForJUnit {

  @Test
  def tokenizeEnglishTest() {
    // Mock the locale used for tokenization.
    val enLocale = new Locale("US", "en")
    // Case 0: Input text is a sequence of blank spaces.
    val text0 = "     "
    var sentences0 = Helper.tokenizeSentences(enLocale, text0)
    // We expect no sentence to be extracted.
    assertTrue(sentences0.size.equals(0))

    // Case 1: Input text begins with whitespace.
    val text1 = " Barack Obama lives in Washington, D.C. . This is another Sentence."
    var sentences1 = Helper.tokenizeSentences(enLocale, text1)
    //  We expect 2 sentences to be extracted.
    assertTrue(sentences1.size.equals(2))
    // Assertions about sentence tokenization.
    assertTrue(text1.subSequence(
      sentences1(0).getStart, sentences1(0).getEnd
      ).equals(" Barack Obama lives in Washington, D.C. . ")
    )
    assertTrue(text1.subSequence(
      sentences1(1).getStart, sentences1(1).getEnd
      ).equals("This is another Sentence.")
    )

    // Case 2: Input text doesn't contain trailing whitespaces or punctuation.
    val text2 = "Sentence without any trailing spaces or punctuation"
    var sentences2 = Helper.tokenizeSentences(enLocale, text2)
    //  We expect 1 sentences to be extracted.
    assertTrue(sentences2.size.equals(1))
    // Assertions about sentence tokenization.
    assertTrue(text2.subSequence(
      sentences2(0).getStart, sentences2(0).getEnd
      ).equals("Sentence without any trailing spaces or punctuation")
    )

    // Case 3: Input text contain lots of whitespace across sentence boundaries.
    val text3 = " This is sentence Uno        .     This is two.  That is sentence drei. "
    var sentences3 = Helper.tokenizeSentences(enLocale, text3)
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

}
