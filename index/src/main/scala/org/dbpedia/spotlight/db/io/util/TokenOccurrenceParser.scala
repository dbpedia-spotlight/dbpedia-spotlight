package org.dbpedia.spotlight.db.io.util


/**
 * A token occurrence parser reads the tokens file that is the result of processing
 * Wikipedia dumps on Pig and converts it for further processing.
 *
 * @author Joachim Daiber
 */

trait TokenOccurrenceParser {
  def parse(tokens: String, minimumCount: Int): Pair[Array[String], Array[Int]]
}

object TokenOccurrenceParser {
  def createDefault: TokenOccurrenceParser = new PigTokenOccurrenceParser()
}



