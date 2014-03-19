package org.dbpedia.spotlight.db

import org.semanticweb.yars.nx.parser.NxParser
import java.io.InputStream
import org.dbpedia.spotlight.log.SpotlightLog
import collection.immutable.ListSet
import scala.Predef._
import org.dbpedia.spotlight.exceptions.NotADBpediaResourceException
import java.net.URLDecoder
import org.dbpedia.spotlight.model.SpotlightConfiguration
import org.dbpedia.extraction.util.WikiUtil
import scala.collection.mutable.ListBuffer

/**
 * Parts of this are taken from
 * org.dbpedia.spotlight.util.ExtractCandidateMap
 *
 * @author Joachim Daiber
 * @author maxjakob
 * @author pablomendes
 */

class WikipediaToDBpediaClosure (
  val namespace: String,
  val redirectsTriples: InputStream,
  val disambiguationTriples: InputStream
) {


  def this(redirectsTriples: InputStream, disambiguationTriples: InputStream) {
    this(SpotlightConfiguration.DEFAULT_NAMESPACE, redirectsTriples, disambiguationTriples)
  }

  SpotlightLog.info(this.getClass, "Loading redirects...")
  var linkMap = Map[String, String]()
  val redParser = new NxParser(redirectsTriples)
  while (redParser.hasNext) {
    val triple = redParser.next
    val subj = URLDecoder.decode(triple(0).toString.replace(namespace, ""),"utf-8")
    val obj  = URLDecoder.decode(triple(2).toString.replace(namespace, ""),"utf-8")
    linkMap  = linkMap.updated(subj, obj)
  }
  SpotlightLog.info(this.getClass, "Done.")

  SpotlightLog.info(this.getClass, "Loading disambiguations...")
  var disambiguationsSet = Set[String]()
  val disParser = new NxParser(disambiguationTriples)
  while (disParser.hasNext) {
    val triple = disParser.next
    val subj = URLDecoder.decode(triple(0).toString.replace(namespace, ""),"utf-8")
    disambiguationsSet  = disambiguationsSet + subj
  }
  SpotlightLog.info(this.getClass, "Done.")


  var wikiToDBPMap = Map[String, String]()
  def this(redirectsTriples: InputStream, disambiguationTriples: InputStream, wikiToDBPTriples: InputStream) {
    this(redirectsTriples, disambiguationTriples)

    SpotlightLog.info(this.getClass, "Reading Wikipedia to DBpediaMapping...")
    val wikiDBPParser = new NxParser(wikiToDBPTriples)
    while (wikiDBPParser.hasNext) {
      val triple = wikiDBPParser.next
      val subj   = URLDecoder.decode(triple(0).toString.replaceFirst("http://[a-z]+[.]wikipedia[.]org/wiki/", ""),"utf-8")
      val obj    = URLDecoder.decode(triple(2).toString.replace(namespace, ""),"utf-8")
      wikiToDBPMap = wikiToDBPMap.updated(subj, getEndOfChainURI(obj))
    }
  }

  val WikiURL = """http://([a-z]+)[.]wikipedia[.]org/wiki/(.*)$""".r
  val DBpediaURL = """http://([a-z]+)[.]dbpedia[.]org/resource/(.*)$""".r

  private def cutOffBeforeAnchor(url: String): String = {
    if(url.contains("%23")) //Take only the part of the URI before the last anchor (#)
      url.take(url.lastIndexOf("%23"))
    else if(url.contains("#"))
      url.take(url.lastIndexOf("#"))
    else
      url
  }

  private def removeLeadingSlashes(url: String): String = {
    url match {
      case t: String if url.startsWith("/") => removeLeadingSlashes(t.tail)
      case t: String => url
    }
  }

  private def wikiToDBpediaURI(wikiURL: String): String = {
    wikiURL match {
      case WikiURL(language, title) =>
        //use only the part before the anchor, URL encode it
        WikiUtil.wikiEncode(
          URLDecoder.decode(removeLeadingSlashes(cutOffBeforeAnchor(title)), "utf-8")
        )
      case DBpediaURL(language, title) => removeLeadingSlashes(cutOffBeforeAnchor(title))
      case _ => throw new NotADBpediaResourceException("Resource is a disambiguation page."); SpotlightLog.error(this.getClass, "Invalid Wikipedia URL %s".format(wikiURL)); null
    }
  }

  def wikipediaToDBpediaURI(wikiURL: String): String = {

    val uri = if(wikiURL.startsWith("http:")){
      if(wikiToDBPMap.size > 0) {
        getEndOfChainURI(wikiToDBPMap(wikiURL))
      } else {
        getEndOfChainURI(wikiToDBpediaURI(wikiURL))
      }
    } else {
      getEndOfChainURI(URLDecoder.decode(wikiURL,"utf-8"))
    }

    if (disambiguationsSet.contains(uri) || uri == null)
      throw new NotADBpediaResourceException("Resource is a disambiguation page.")
    else
      uri
  }

  def getEndOfChainURI(uri: String): String = {
    getEndOfChainURI(uri,Set(uri))
  }

  private def getEndOfChainURI(uri: String, alreadyTraversed:Set[String]): String = {
    linkMap.get(uri) match {
      case Some(s: String) => if (alreadyTraversed.contains(s)) uri else getEndOfChainURI(s, alreadyTraversed + s)
      case None => uri
    }
  }
}
