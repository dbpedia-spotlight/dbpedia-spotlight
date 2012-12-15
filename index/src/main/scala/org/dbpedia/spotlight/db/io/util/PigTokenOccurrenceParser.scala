package org.dbpedia.spotlight.db.io.util

class PigTokenOccurrenceParser extends TokenOccurrenceParser {

  def parse(tokens: String): Pair[Array[String], Array[Int]] = {
    var tokensA = Array[String]()
    var countsA = Array[Int]()

    tokens.tail.init.split("[()]").filter(pair => !pair.equals(",") && !pair.equals("")).map {
      pair: String => {
        val i = pair.lastIndexOf(',')
        tokensA :+= pair.take(i)
        countsA :+= pair.drop(i+1).toInt
      }
    }
    Pair(tokensA, countsA)
  }
}