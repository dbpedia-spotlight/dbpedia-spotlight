package org.dbpedia.spotlight.db.similarity

import org.dbpedia.spotlight.db.memory.MemoryQuantizedCountStore
import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}
import org.dbpedia.spotlight.Helper.{createContextStore, createTokenTypeStore}
import org.junit.Assert._
import org.junit.Test


class TestGenerativeContextSimilarity {

  @Test
  def testIntersectWithStopWordToken() {

    val res = new DBpediaResource("some uri")
    val token0 = new TokenType(0, "token0", 3)
    val token1 = new TokenType(1, "token1", 5)
    val token2 = new TokenType(2, "token2", 4)
    val occs = List((res, Array(token0, token1), Array(10, 12)))

    val tokenTypeStore = createTokenTypeStore(List(token0, token1, token2))
    val quantizedCountStore = new MemoryQuantizedCountStore()
    val contextStore = createContextStore(occs, tokenTypeStore, quantizedCountStore)
    val gcs = new GenerativeContextSimilarity(tokenTypeStore, contextStore)

    val query = List(TokenType.STOPWORD, TokenType.UNKNOWN, token1, token2)
    val intersection = gcs.intersect(query, res).filterNot { case (token, count) => count == 0 }
    assertEquals(Seq((token1, 12)), intersection)
  }

}
