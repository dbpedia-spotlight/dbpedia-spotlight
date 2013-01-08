package org.dbpedia.spotlight.util

import scala.collection.mutable.Map
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.{Analyzer, PorterStemFilter, TokenStream}
import java.io.{Reader, StringReader}
import org.apache.lucene.analysis.standard.{StandardTokenizer, StandardAnalyzer}
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.snowball.SnowballFilter
import org.tartarus.snowball.SnowballProgram
import java.util
import org.dbpedia.spotlight.lucene.analysis.PhoneticAnalyzer

/**
 * This class converts tectual input into stemmed and filtered word count vectors utilizing lucenes EnglishAnalyzer or
 * our PhoneticAnalyzer
 * @param isPhonetic if phonetic analyzer should be used
 *
 * @author dirk
 */
class TextVectorizer(isPhonetic: Boolean = false) {
  private val stopwords = new util.HashSet[String](util.Arrays.asList(
    "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"
  ))

  val analyzer: Analyzer = if (isPhonetic) new PhoneticAnalyzer(Version.LUCENE_36, stopwords) else new EnglishAnalyzer(Version.LUCENE_36, stopwords)
  var token: String = null
  var tokenStream: TokenStream = analyzer.reusableTokenStream(null, new StringReader(""))
  var charTermAttribute: CharTermAttribute = null

  def getWordCountVector(document: String): Map[String, Double] = {
    var result: Map[String, Double] = Map()

    tokenStream = analyzer.reusableTokenStream(null, new StringReader(document))
    charTermAttribute = tokenStream.addAttribute(classOf[CharTermAttribute])

    while (tokenStream.incrementToken()) {
      token = charTermAttribute.toString()
      if (token.matches("[a-zA-Z]{3,}")) {
        if (result.contains(token)) {
          result(token) += 1
        }
        else
          result += (token -> 1)
      }
    }

    result
  }

}
