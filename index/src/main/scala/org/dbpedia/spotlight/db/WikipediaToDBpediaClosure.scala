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

  private def decodeURL(uri: String) = URLDecoder.decode(uri,"utf-8")

  SpotlightLog.info(this.getClass, "Loading redirects...")
  var linkMap = Map[String, String]()
  val redParser = new NxParser(redirectsTriples)
  while (redParser.hasNext) {
    val triple = redParser.next
    val subj = decodeURL(triple(0).toString.replace(namespace, ""))
    val obj  = decodeURL(triple(2).toString.replace(namespace, ""))
    linkMap  = linkMap.updated(subj, obj)
  }
  SpotlightLog.info(this.getClass, "Done.")

  SpotlightLog.info(this.getClass, "Loading disambiguations...")
  var disambiguationsSet = Set[String]()
  val disParser = new NxParser(disambiguationTriples)
  while (disParser.hasNext) {
    val triple = disParser.next
    val subj = decodeURL(triple(0).toString.replace(namespace, ""))
    disambiguationsSet  = disambiguationsSet + subj
  }
  SpotlightLog.info(this.getClass, "Done.")

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

  /**
   * Use only the part before the anchor, ensure the URL encoding is correct.
   *
   * @param url the full DBpedia or Wikipedia URL
   * @return
   */
  private def decodedNameFromURL(url: String): String = url match {
    case WikiURL(language, title) =>  WikiUtil.wikiEncode(decodeURL(removeLeadingSlashes(cutOffBeforeAnchor(title))))
    case DBpediaURL(language, title) => decodeURL(removeLeadingSlashes(cutOffBeforeAnchor(title)))
    case _ => throw new NotADBpediaResourceException("Resource is a disambiguation page."); SpotlightLog.error(this.getClass, "Invalid Wikipedia URL %s", url); null
  }

  /**
   * Get the end of the redirect chain for the URL or name (last part of the URL).
   *
   * @param url either full Wiki or DBpedia URL or only the name
   * @return
   */
  def wikipediaToDBpediaURI(url: String): String = {

    val uri = if(url.startsWith("http:")) {
      getEndOfChainURI(decodedNameFromURL(url))
    } else {
      getEndOfChainURI(decodeURL(url))
    }

    if (disambiguationsSet.contains(uri) || uri == null)
      throw new NotADBpediaResourceException("Resource is a disambiguation page.")
    else
      uri
  }

  def getEndOfChainURI(uri: String): String = getEndOfChainURI(uri, Set(uri))

  private def getEndOfChainURI(uri: String, alreadyTraversed:Set[String]): String = linkMap.get(uri) match {
    case Some(s: String) => if (alreadyTraversed.contains(s)) uri else getEndOfChainURI(s, alreadyTraversed + s)
    case None => uri
  }

}