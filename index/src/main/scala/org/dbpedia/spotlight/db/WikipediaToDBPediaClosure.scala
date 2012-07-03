package org.dbpedia.spotlight.db

import org.semanticweb.yars.nx.parser.NxParser
import org.dbpedia.spotlight.model.DBpediaResource
import java.io.InputStream
import org.apache.commons.logging.LogFactory
import collection.immutable.ListSet
import scala.Predef._
import org.dbpedia.spotlight.exceptions.NotADBpediaResourceException

/**
 * Parts of this are taken from
 * org.dbpedia.spotlight.util.ExtractCandidateMap
 *
 * @author Joachim Daiber
 * @author maxjakob
 * @author pablomendes
 */

class WikipediaToDBpediaClosure (
  val redirectsTriples: InputStream,
  val disambiguationTriples: InputStream
) {
  private val LOG = LogFactory.getLog(this.getClass)

  LOG.info("Loading redirects...")
  var linkMap = Map[String, String]()
  val redParser = new NxParser(redirectsTriples)
  while (redParser.hasNext) {
    val triple = redParser.next
    val subj = triple(0).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
    val obj  = triple(2).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
    linkMap  = linkMap.updated(subj, obj)
  }
  LOG.info("Done.")

  LOG.info("Loading disambiguations...")
  var disambiguationsSet = Set[String]()
  val disParser = new NxParser(disambiguationTriples)
  while (disParser.hasNext) {
    val triple = redParser.next
    val subj = triple(0).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
    disambiguationsSet  = disambiguationsSet + subj
  }
  LOG.info("Done.")


  var wikiToDBPMap = Map[String, String]()
  def this(redirectsTriples: InputStream, disambiguationTriples: InputStream, wikiToDBPTriples: InputStream) {
    this(redirectsTriples, disambiguationTriples)

    LOG.info("Reading Wikipedia to DBpediaMapping...")
    val wikiDBPParser = new NxParser(wikiToDBPTriples)
    while (wikiDBPParser.hasNext) {
      val triple = wikiDBPParser.next
      val subj   = triple(0).toString.replaceFirst("http://[a-z]+[.]wikipedia[.]org/wiki/", "")
      val obj    = triple(2).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
      wikiToDBPMap = wikiToDBPMap.updated(subj, getEndOfChainURI(linkMap, obj))
    }
  }


  val WikiURL = """http://([a-z]+)[.]wikipedia[.]org/wiki/(.*)$""".r
  private def wikiToDBpediaURI(wikiURL: String): String = {
    wikiURL match {
      case WikiURL(language, title) => {
        title.replace("(", "%28").replace(")", "%29").takeWhile( p => p != '#' ) match {
          case t: String if t.startsWith("/") => t.tail
          case t: String => t
        }
      }
      case _ => LOG.error("Invalid Wikipedia URL %s".format(wikiURL)); null
    }
  }

  def wikipediaToDBpediaURI(wikiURL: String): String = {

    val uri = if(wikiToDBPMap.size > 0) {
      getEndOfChainURI(linkMap, wikiToDBPMap(wikiURL))
    } else {
      getEndOfChainURI(linkMap, wikiToDBpediaURI(wikiURL))
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
