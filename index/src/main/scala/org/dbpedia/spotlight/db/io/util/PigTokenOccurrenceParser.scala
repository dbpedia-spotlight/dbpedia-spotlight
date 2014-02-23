package org.dbpedia.spotlight.db.io.util

class PigTokenOccurrenceParser extends TokenOccurrenceParser {

  def parse(tokens: String, minimumCount: Int): Pair[Array[String], Array[Int]] = {
    var tokensA = Array[String]()
    var countsA = Array[Int]()

    tokens.tail.init.split("[()]").filter(pair => !pair.equals(",") && !pair.equals("")).map {
      pair: String => {
        val i = pair.lastIndexOf(',')
        val count = pair.drop(i+1).toInt

        if (count >= minimumCount) {
          tokensA :+= pair.take(i)
          countsA :+= pair.drop(i+1).toInt
        }
      }
    }
    Pair(tokensA, countsA)
  }
}