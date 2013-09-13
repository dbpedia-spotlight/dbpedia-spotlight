package org.dbpedia.spotlight.db.io

import java.io.{InputStream, FileInputStream, File}
import io.Source
import org.dbpedia.spotlight.db.WikipediaToDBpediaClosure
import org.dbpedia.spotlight.db.model.{ResourceStore, TokenTypeStore}
import org.dbpedia.spotlight.log.SpotlightLog
import scala.Predef._
import scala.Array
import org.dbpedia.spotlight.exceptions.{DBpediaResourceNotFoundException, NotADBpediaResourceException}
import org.dbpedia.spotlight.model.{TokenType, DBpediaResource}
import util.TokenOccurrenceParser


/**
 * A source for token occurrences.
 *
 * @author Joachim Daiber
 */

object TokenOccurrenceSource {

  def fromPigInputStream(tokenInputStream: InputStream, tokenTypeStore: TokenTypeStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore): Iterator[Triple[DBpediaResource, Array[TokenType], Array[Int]]] = {

    var i = 0
    plainTokenOccurrenceSource(tokenInputStream) map {
      case (wikiurl: String, tokens: Array[String], counts: Array[Int]) => {
        i += 1
        if (i % 10000 == 0)
          SpotlightLog.info(this.getClass, "Read context for %d resources...", i)
        try {
          Triple(
            resStore.getResourceByName(wikipediaToDBpediaClosure.wikipediaToDBpediaURI(wikiurl)),
            tokens.map{ token => tokenTypeStore.getTokenType(token) },
            counts
          )
        } catch {
          case e: DBpediaResourceNotFoundException => Triple(null, null, null)
          case e: NotADBpediaResourceException     => Triple(null, null, null)
        }
      }
    }

  }

  def fromPigFile(tokenFile: File, tokenStore: TokenTypeStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore) = fromPigInputStream(new FileInputStream(tokenFile), tokenStore, wikipediaToDBpediaClosure, resStore)

  val tokensParser = TokenOccurrenceParser.createDefault

  def plainTokenOccurrenceSource(tokenInputStream: InputStream): Iterator[Triple[String, Array[String], Array[Int]]] = {
    Source.fromInputStream(tokenInputStream) getLines() filter(!_.equals("")) map {
      line: String => {
        val Array(wikiurl, tokens) = line.trim().split('\t')
        val Pair(tokensA, countsA) = tokensParser.parse(tokens)
        Triple(wikiurl, tokensA, countsA)
      }
    }
  }
}
