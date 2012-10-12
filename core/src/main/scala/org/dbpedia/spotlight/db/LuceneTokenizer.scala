package org.dbpedia.spotlight.db

import model.Tokenizer
import org.dbpedia.spotlight.model.Text
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import java.io.{IOException, StringReader}
import collection.mutable.ListBuffer
import scala.collection.JavaConversions._

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class LuceneTokenizer(analyzer: Analyzer) extends Tokenizer {

  def tokenize(text: Text): List[String] = {
    val tokenStream = analyzer.tokenStream(null, new StringReader(text.text))
    val tokens = ListBuffer[String]()

    try {
      while(tokenStream.incrementToken()) {
        tokens += tokenStream.getAttribute(classOf[CharTermAttribute]).toString;
      }
    } catch {
      case e: IOException => //Ignoring the exception since we use StringReader
    }

    tokens.toList
  }

}
