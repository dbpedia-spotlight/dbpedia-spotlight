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


  //http://en.wikipedia.org/wiki/0  {(0,20),(ε,9),(00,8),(1,6),(space,6),(redirect,6),(link,5),(net,5),(2,3,7,4),(number,4),(delet,4),(ω,4),(bot,4),(σ,4),(targetnam,3),(broken,3),(creat,3),(prefer,3),(pretzel,2),(hyperbol,2),(finit,2),(year
  def fromPigInputStream(tokenInputStream: InputStream, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore) = Iterator[DBpediaResource, Array[Int, Int]] {

    plainTokenOccurrenceSource(tokenInputStream) map {
      case (wikiurl: String, tokens: Array[Pair[String, Int]]) => {
        Pair(
          resStore.getResourceByName(wikipediaToDBpediaClosure.wikipediaToDBpediaURI(wikiurl)),
          tokens.map{ case (token, count) => (tokenStore.getToken(token), count) }
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
