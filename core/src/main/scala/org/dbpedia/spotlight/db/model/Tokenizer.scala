package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.Text
import java.util.List


/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait Tokenizer {

  def tokenize(text: Text): List[String]

}
