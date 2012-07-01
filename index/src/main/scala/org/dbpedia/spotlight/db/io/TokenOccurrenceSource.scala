package org.dbpedia.spotlight.db.io

import java.io.{InputStream, FileInputStream, File}
import io.Source
import org.dbpedia.spotlight.model.DBpediaResource
import org.dbpedia.spotlight.db.WikipediaToDBpediaClosure
import org.dbpedia.spotlight.db.model.{ResourceStore, TokenStore}


/**
 * @author Joachim Daiber
 *
 *
 *
 */

object TokenOccurrenceSource {


  def fromPigInputStream(tokenInputStream: InputStream, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore): Iterator[Pair[DBpediaResource, Array[Pair[Int, Int]]]] = {

    plainTokenOccurrenceSource(tokenInputStream) map {
      case (wikiurl: String, tokens: Array[Pair[String, Int]]) => {
        Pair(
          resStore.getResourceByName(wikipediaToDBpediaClosure.wikipediaToDBpediaURI(wikiurl)),
          tokens.map{ case (token, count) => (tokenStore.getToken(token).id, count) }
        )
      }
    }

  }

  def fromPigFile(tokenFile: File, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore) = fromPigInputStream(new FileInputStream(tokenFile), tokenStore, wikipediaToDBpediaClosure, resStore)


  def plainTokenOccurrenceSource(tokenInputStream: InputStream): Iterator[Pair[String, Array[Pair[String, Int]]]] = Source.fromInputStream(tokenInputStream).getLines() filter(!_.equals("")) map {
    line: String => {
      val Array(wikiurl, tokens) = line.trim().split('\t')
      Pair(
        wikiurl,
        tokens.tail.init.split("[()]").filter(pair => !pair.equals(",") && !pair.equals("")).map {
          pair: String => {
            val i = pair.lastIndexOf(',')
            (pair.take(i), pair.drop(i+1).toInt)
          }
        }
      )
    }
  }
}
