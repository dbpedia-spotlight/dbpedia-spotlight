package org.dbpedia.spotlight.db.io

import org.dbpedia.spotlight.io.OccurrenceSource
import org.dbpedia.spotlight.db.model.{StringTokenizer, SurfaceFormStore}
import collection.mutable.HashMap
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.io.{InputStream, FileInputStream, File}
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.model._


/**
 * A source for tokens.
 *
 * TODO: Note that this object is currently summing total counts for tokens over all token occurrences. Eventually,
 * this should be moved to Apache Pig.
 *
 * @author Joachim Daiber
 */

object TokenSource {

  private val ADDITIONAL_TOKEN_COUNT = 1

  def fromSFStore(sfStore: SurfaceFormStore, tokenizer: StringTokenizer): Seq[String] = {
    sfStore.iterateSurfaceForms.grouped(100000).toList.par.flatMap(_.map{
      sf: SurfaceForm =>
        //Tokenize all SFs first
        tokenizer.tokenize(sf.name)
    }).seq.flatten
  }

  def fromPigFile(tokenFile: File, additionalTokens: Option[Seq[String]] = None) = fromPigInputStream(new FileInputStream(tokenFile), additionalTokens)
  def fromPigInputStream(tokenFile: InputStream, additionalTokens: Option[Seq[String]] = None) = {

    val tokenMap = HashMap[String, Int]()

    var i = 0
    TokenOccurrenceSource.plainTokenOccurrenceSource(tokenFile) foreach {
      p: Triple[String, Array[String], Array[Int]] => {
        i += 1
        if (i % 10000 == 0)
          SpotlightLog.info(this.getClass, "Read context for %d resources...", i)

        (0 to p._2.size -1).foreach {
          i: Int => tokenMap.put(p._2(i), tokenMap.getOrElse(p._2(i), 0) + p._3(i))
        }
      }
    }

    additionalTokens match {
      case Some(tokens) => {
        SpotlightLog.info(this.getClass, "Read %d additional tokens...", tokens.size)
        tokens.foreach { token: String =>
          tokenMap.put(token, tokenMap.getOrElse(token, 0) + ADDITIONAL_TOKEN_COUNT)
        }
      }
      case None =>
    }

    var id = -1
    tokenMap.map{
      case(token, count) => {
        id += 1
        (new TokenType(id, token, count), count)
      }
    }.toMap.asJava
  }

}
