package org.dbpedia.spotlight.db.io.util

class RegexTokenOccurrenceParser extends TokenOccurrenceParser {

  def parse(tokens: String): Pair[Array[String], Array[Int]] = {
    var tokensA = Array[String]()
    var countsA = Array[Int]()

    tokens.tail.init.split("(\\[\"|\",|\\])").filter(pair => !pair.equals(",") && !pair.equals("")).grouped(2).foreach {
      case Array(a, b) => {
        tokensA :+= a
        countsA :+= b.toInt
      }
    }
    Pair(tokensA, countsA)
  }

}