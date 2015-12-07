package org.dbpedia.spotlight.util

import org.dbpedia.spotlight.db.memory.{MemoryContextStore, MemoryQuantizedCountStore, MemoryTokenTypeStore}
import org.dbpedia.spotlight.db.model.TokenTypeStore
import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}


object MemoryStoreUtil {

  def createTokenTypeStore(tokenTypes: List[TokenType]): MemoryTokenTypeStore = {

    val tokenTypeStore = new MemoryTokenTypeStore()
    val tokens = new Array[String](tokenTypes.size + 1)
    val counts = new Array[Int](tokenTypes.size + 1)

    tokenTypes.foreach(token => {
      tokens(token.id) = token.tokenType
      counts(token.id) = token.count
    })

    tokenTypeStore.tokenForId = tokens
    tokenTypeStore.counts = counts
    tokenTypeStore.loaded()

    tokenTypeStore
  }

  def createContextStore(occs: List[(DBpediaResource, Array[TokenType], Array[Int])], tokenStore: TokenTypeStore,
                         quantizedCountStore: MemoryQuantizedCountStore): MemoryContextStore = {

    val numResources = occs.size
    val tokens = new Array[Array[Int]](numResources)
    val counts = new Array[Array[Short]](numResources)

    occs.foreach { case (res, uriTokens, uriCounts) =>
      tokens(res.id) = uriTokens.map(_.id)
      counts(res.id) = uriCounts.map { count => quantizedCountStore.addCount(count) }
    }

    val contextStore = new MemoryContextStore()
    contextStore.tokens = tokens
    contextStore.counts = counts
    contextStore.tokenStore = tokenStore
    contextStore.quantizedCountStore = quantizedCountStore
    contextStore.totalTokenCounts = new Array[Int](numResources)
    contextStore.calculateTotalTokenCounts()
    contextStore
  }

}
