package org.dbpedia.spotlight.db.tokenize

import org.dbpedia.spotlight.db.model.{RawTokenizer, Stemmer}
import org.dbpedia.spotlight.model.Text

abstract class BaseRawTokenizer(stemmer: Stemmer) extends RawTokenizer {

  protected def tokenizeUnstemmed(text: String): Seq[String]
  def tokenize(text: String): Seq[String] = tokenizeUnstemmed(text).map( stemmer.stem(_) )
  def tokenize(text: Text): Seq[String] = tokenize(text.text)

}
