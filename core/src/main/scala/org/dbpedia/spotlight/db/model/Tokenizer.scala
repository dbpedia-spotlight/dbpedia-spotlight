package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.Text


/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait Tokenizer {

  def tokenize(text: Text): List[String]

}
