package org.dbpedia.spotlight.db.io.util

import org.codehaus.jackson.{JsonToken, JsonFactory}

/**
 * TokenOccurrenceParser based on the Jackson Streaming API.
 *
 * @author Joachim Daiber
 */
class JacksonTokenOccurrenceParser extends TokenOccurrenceParser {

  val jFactory = new JsonFactory()

  def parse(tokens: String): Pair[Array[String], Array[Int]] = {
    var tokensA = Array[String]()
    var countsA = Array[Int]()

    val jParser = jFactory.createJsonParser(tokens)

    jParser.nextToken()
    while (jParser.nextToken() != JsonToken.END_ARRAY) {

      jParser.nextToken()
      tokensA :+= jParser.getText

      jParser.nextToken()
      countsA :+= jParser.getIntValue

      jParser.nextToken()
    }

    Pair(tokensA, countsA)
  }

}