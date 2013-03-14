package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.Text
import opennlp.tools.util.Span

trait RawTokenizer {

  def tokenize(text: Text): Seq[String]

  def tokenize(text: String): Seq[String]
  def tokenizePos(text: String): Array[Span]

}
