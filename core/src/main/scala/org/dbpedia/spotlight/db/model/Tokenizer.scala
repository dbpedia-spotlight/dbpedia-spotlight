package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{Token, Text}


/**
 * A Tokenizer splits a [[org.dbpedia.spotlight.model.Text]] into its [[org.dbpedia.spotlight.model.Token]]s.
 * Tokens may have additional information, e.g. part-of-speech tags and the [[org.dbpedia.spotlight.model.Text]]
 * may be assigned features like the sentence boundaries.
 *
 * @author Joachim Daiber
 */

trait Tokenizer {

  /**
   * Tokenize the text, return the Token objects. Features may be assigned to the [[org.dbpedia.spotlight.model.Text]]
   * object.
   * @param text the text to be tokenized
   * @return
   */
  def tokenize(text: Text): List[Token]

}
