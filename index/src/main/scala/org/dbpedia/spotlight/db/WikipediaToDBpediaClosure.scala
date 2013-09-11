package org.dbpedia.spotlight.db

import org.semanticweb.yars.nx.parser.NxParser
import java.io.InputStream
import org.dbpedia.spotlight.log.SpotlightLog
import collection.immutable.ListSet
import scala.Predef._
import org.dbpedia.spotlight.exceptions.NotADBpediaResourceException
import java.net.{URLDecoder, URLEncoder}
import org.dbpedia.spotlight.model.{SpotlightConfiguration, DBpediaResource}
import org.dbpedia.extraction.util.WikiUtil

/**
 * Parts of this are taken from
 * org.dbpedia.spotlight.util.ExtractCandidateMap
 *
 * @author Joachim Daiber
 * @author maxjakob
 * @author pablomendes
 */

class WikipediaToDBpediaClosure(
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
    val subj = triple(0).toString.replace(namespace, "")
    val obj = triple(2).toString.replace(namespace, "")
    linkMap = linkMap.updated(subj, obj)
  }
  SpotlightLog.info(this.getClass, "Done.")

  SpotlightLog.info(this.getClass, "Loading disambiguations...")
  var disambiguationsSet = Set[String]()
  val disParser = new NxParser(disambiguationTriples)
  while (disParser.hasNext) {
    val triple = disParser.next
    val subj = triple(0).toString.replace(namespace, "")
    disambiguationsSet = disambiguationsSet + subj
  }
  SpotlightLog.info(this.getClass, "Done.")


  var wikiToDBPMap = Map[String, String]()

  def this(redirectsTriples: InputStream, disambiguationTriples: InputStream, wikiToDBPTriples: InputStream) {
    this(redirectsTriples, disambiguationTriples)

    SpotlightLog.info(this.getClass, "Reading Wikipedia to DBpediaMapping...")
    val wikiDBPParser = new NxParser(wikiToDBPTriples)
    while (wikiDBPParser.hasNext) {
      val triple = wikiDBPParser.next
      val subj = triple(0).toString.replaceFirst("http://[a-z]+[.]wikipedia[.]org/wiki/", "")
      val obj = triple(2).toString.replace(namespace, "")
      wikiToDBPMap = wikiToDBPMap.updated(subj, getEndOfChainURI(linkMap, obj))
    }
  }

  val WikiURL = """http://([a-z]+)[.]wikipedia[.]org/wiki/(.*)$""".r

  private def wikiToDBpediaURI(wikiURL: String): String = {
    wikiURL match {
      case WikiURL(language, title) => {

        //use only the part before the anchor, URL encode it
        WikiUtil.wikiEncode(
          URLDecoder.decode(
            title.takeWhile(p => p != '#') match {
              case t: String if t.startsWith("/") => t.tail
              case t: String => t
            }, "utf-8"))
      }
      case _ => SpotlightLog.error(this.getClass, "Invalid Wikipedia URL %s", wikiURL); null
    }
  }

  def wikipediaToDBpediaURI(wikiURL: String): String = {

    val uri = if (wikiURL.startsWith("http:")) {
      if (wikiToDBPMap.size > 0) {
        getEndOfChainURI(linkMap, wikiToDBPMap(wikiURL))
      } else {
        getEndOfChainURI(linkMap, wikiToDBpediaURI(wikiURL))
      }
    } else {
      getEndOfChainURI(linkMap, wikiURL)
    }

    if (disambiguationsSet.contains(uri))
      throw new NotADBpediaResourceException("Resource is a disambiguation page.")
    else
      uri
  }

  def getEndOfChainURI(m: Map[String, String], uri: String): String = {
    getURIChain(m, ListSet(uri)).last
  }

  private def getURIChain(m: Map[String, String], chain: ListSet[String]): ListSet[String] = {
    // get end of chain but check for redirects to itself
    m.get(chain.last) match {
      case Some(s: String) => if (chain.contains(s)) chain else getURIChain(m, chain + s)
      case None => chain
    }
  }
}
