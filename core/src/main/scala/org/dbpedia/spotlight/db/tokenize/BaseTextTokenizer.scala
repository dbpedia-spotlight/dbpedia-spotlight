package org.dbpedia.spotlight.db.tokenize

import org.dbpedia.spotlight.model.{TokenType, Feature, Token, Text}
import org.dbpedia.spotlight.db.model.{StringTokenizer, TextTokenizer, Stemmer, TokenTypeStore}


abstract class BaseTextTokenizer(tokenTypeStore: TokenTypeStore, stemmer: Stemmer) extends TextTokenizer {

  def tokenize(text: Text): List[Token]

  def tokenizeMaybe(text: Text) {
    if(text.feature("tokens").isEmpty)
      text.setFeature(new Feature("tokens", tokenize(text)))
  }

  protected def getStemmedTokenType(token: String): TokenType = tokenTypeStore.getTokenType(stemmer.stem(token))

  def getStringTokenizer: StringTokenizer

}
