package org.dbpedia.spotlight.db.io

import org.dbpedia.spotlight.io.OccurrenceSource
import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, Token}
import org.dbpedia.spotlight.db.model.Tokenizer
import collection.mutable.HashMap
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.io.{InputStream, FileInputStream, File}


/**
 * @author Joachim Daiber
 *
 *
 *
 */

object TokenSource {

  def fromOccurrenceSource(os: OccurrenceSource, tokenizer: Tokenizer): java.util.Map[Token, Int] = {
    val tokenMap = HashMap[String, Int]()

    os.foreach {
      occ: DBpediaResourceOccurrence => {
        tokenizer.tokenize(occ.context) foreach {
          token: String => tokenMap.put(token, tokenMap.getOrElse(token, 0) + 1)
        }
      }
    }

    var id = -1
    tokenMap.map{
      case(token, count) => {
        id += 1
        (new Token(id, token, count), count)
      }
    }.toMap.asJava
  }

  def fromPigFile(tokenFile: File) = fromPigInputStream(new FileInputStream(tokenFile))
  def fromPigInputStream(tokenFile: InputStream) = {

    val tokenMap = HashMap[String, Int]()

    TokenOccurrenceSource.plainTokenOccurrenceSource(tokenFile) foreach {
      p: Pair[String, Array[Pair[String, Int]]] => {
        p._2.foreach {
          case (token, count) => tokenMap.put(token, tokenMap.getOrElse(token, 0))
        }
      }
    }

    var id = -1
    tokenMap.map{
      case(token, count) => {
        id += 1
        (new Token(id, token, count), count)
      }
    }.toMap.asJava
  }

}
