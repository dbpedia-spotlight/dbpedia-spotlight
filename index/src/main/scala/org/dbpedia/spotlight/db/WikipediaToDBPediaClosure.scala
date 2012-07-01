package org.dbpedia.spotlight.db

import memory.MemoryStore
import org.dbpedia.spotlight.util.ExtractCandidateMap._
import scala.io.Source
import org.semanticweb.yars.nx.parser.NxParser
import org.dbpedia.spotlight.model.DBpediaResource
import java.io.{InputStream, PrintStream, FileInputStream, File}
import org.apache.commons.logging.LogFactory
import scala.Predef._
import collection.immutable.{ListSet, SortedSet}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class WikipediaToDBpediaClosure (
  val redirectsTriples: InputStream
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

  var wikiToDBPMap = Map[String, String]()
  def this(redirectsTriples: InputStream, wikiToDBPTriples: InputStream) {
    this(redirectsTriples: InputStream)

    LOG.info("Reading Wikipedia to DBpediaMapping...")
    val wikiDBPParser = new NxParser(wikiToDBPTriples)
    while (wikiDBPParser.hasNext) {
      val triple = wikiDBPParser.next
      val subj   = triple(0).toString.replaceFirst("http://[a-z]+[.]wikipedia[.]org/wiki/", "")
      val obj    = triple(2).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
      wikiToDBPMap = wikiToDBPMap.updated(subj, getEndOfChainURI(linkMap, obj))
    }
  }

  private def wikiToDBpediaURI(wikiURL: String): String = {
    wikiURL.replaceFirst("http://[a-z]+[.]wikipedia[.]org/wiki/", "")
  }

  def wikipediaToDBpediaURI(wikiURL: String): String = {
    if(wikiToDBPMap.size > 0)
      getEndOfChainURI(linkMap, wikiToDBPMap(wikiURL))
    else
      getEndOfChainURI(linkMap, wikiToDBpediaURI(wikiURL))
  }

  def getEndOfChainURI(m: Map[String, String], uri: String): String = {
    getURIChain(m, ListSet(uri)).last
  }


  private def getURIChain(m: Map[String, String], chain: ListSet[String]): ListSet[String] = {
      // get end of chain but check for redirects to itself
      m.get(chain.head) match {
          case Some(s: String) => if (chain.contains(s)) chain else getURIChain(m, chain + s)
          case None => chain
      }
  }


}
