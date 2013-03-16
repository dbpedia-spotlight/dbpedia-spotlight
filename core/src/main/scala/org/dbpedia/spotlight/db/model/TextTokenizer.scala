package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{Token, Text}

/**
 * A Tokenizer splits a [[org.dbpedia.spotlight.model.Text]] into its [[org.dbpedia.spotlight.model.Token]]s.
 * Tokens may have additional information, e.g. part-of-speech tags and the [[org.dbpedia.spotlight.model.Text]]
 * may be assigned features like the sentence boundaries.
 *
 * @author Joachim Daiber
 */

trait TextTokenizer {

  def tokenize(text: Text): List[Token]

  def tokenizeMaybe(text: Text)

  def getStringTokenizer: StringTokenizer

}
