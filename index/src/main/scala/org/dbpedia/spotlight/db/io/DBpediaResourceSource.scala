package org.dbpedia.spotlight.db.io

import io.Source
import java.io.{File, FileInputStream, InputStream}
import org.dbpedia.spotlight.model.Factory.OntologyType
import collection.immutable.HashMap
import scala.collection.JavaConverters._
import java.util.NoSuchElementException
import scala.collection.mutable.HashSet
import org.dbpedia.spotlight.lucene.index.ExtractOccsFromWikipedia._
import org.dbpedia.spotlight.filter.occurrences.RedirectResolveFilter
import org.dbpedia.spotlight.db.WikipediaToDBpediaClosure
import org.apache.commons.logging.LogFactory
import scala.Predef._
import scala.Array
import org.dbpedia.spotlight.model._
import collection.parallel.mutable
import org.dbpedia.spotlight.exceptions.NotADBpediaResourceException
import org.semanticweb.yars.nx.parser.NxParser


/**
 * Represents a source of DBpediaResources.
 *
 * Type definitions must be prepared beforehand, see
 *  src/main/scripts/types.sh
 *
 * @author Joachim Daiber
 */

object DBpediaResourceSource {

  private val LOG = LogFactory.getLog(this.getClass)

  def fromTSVInputStream(
    conceptList: InputStream,
    counts: InputStream,
    instanceTypes: InputStream
    ): java.util.Map[DBpediaResource, Int] = {

    var id = 0

    //The list of concepts may contain non-unique elements, hence convert it to a Set first to make sure
    //we do not count elements more than once.

    var linesIterator : Iterator[String] = Iterator.empty
    try {
      linesIterator = Source.fromInputStream(conceptList, "UTF-8").getLines
    } catch {
      case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(conceptList).getLines
      }

    val resourceMap: Map[String, DBpediaResource] = (linesIterator.toSet map {
      line: String => {
        val res = new DBpediaResource(line.trim)
        res.id = id
        id += 1
        Pair(res.uri, res)
      }
    }).toMap

    //Read counts:

    linesIterator = Iterator.empty
    try {
      linesIterator = Source.fromInputStream(counts, "UTF-8").getLines
    } catch {
      case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(counts).getLines
      }

    for (line <- linesIterator) {
        val Array(uri: String, count: String) = line.trim().split('\t')
        resourceMap(new DBpediaResource(uri).uri).setSupport(count.toInt)
    }

    //Read types:
    val uriNotFound = HashSet[String]()

    linesIterator = Iterator.empty
    try {
      linesIterator = Source.fromInputStream(instanceTypes, "UTF-8").getLines
    } catch {
      case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(instanceTypes).getLines
      }

    for (line <- linesIterator) {
        val Array(id: String, typeURI: String) = line.trim().split('\t')

        try {
          resourceMap(new DBpediaResource(id).uri).types ::= OntologyType.fromURI(typeURI)
        } catch {
          case e: NoSuchElementException =>
            //System.err.println("WARNING: DBpedia resource not in concept list %s (%s)".format(id, typeURI) )
            uriNotFound += id
        }
    }
    LOG.warn("URI for %d type definitions not found!".format(uriNotFound.size) )

    resourceMap.iterator.map( f => Pair(f._2, f._2.support) ).toMap.asJava
  }

  def fromPigInputStreams(
    wikipediaToDBpediaClosure: WikipediaToDBpediaClosure,
    resourceCounts: InputStream,
    instanceTypes: (String, InputStream),
    namespace: String
  ): java.util.Map[DBpediaResource, Int] = {

    LOG.info("Creating DBepdiaResourceSource.")

    var id = 1

    val resourceMap = new java.util.HashMap[DBpediaResource, Int]()
    val resourceByURI = scala.collection.mutable.HashMap[String, DBpediaResource]()

    LOG.info("Reading resources+counts...")

    var linesIterator : Iterator[String] = Iterator.empty
    try {
      linesIterator = Source.fromInputStream(resourceCounts, "UTF-8").getLines
    } catch {
      case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(resourceCounts).getLines
      }

    for (line <- linesIterator) {
        try {
          val Array(wikiurl, count) = line.trim().split('\t')
          val res = new DBpediaResource(wikipediaToDBpediaClosure.wikipediaToDBpediaURI(wikiurl))

          resourceByURI.get(res.uri) match {
            case Some(oldRes) => {
              oldRes.setSupport(oldRes.support + count.toInt)
              resourceByURI.put(oldRes.uri, oldRes)
            }
            case None => {
              res.id = id
              id += 1
              res.setSupport(count.toInt)
              resourceByURI.put(res.uri, res)
            }
          }
        } catch {
          case e: NotADBpediaResourceException => //Ignore Disambiguation pages
        }
    }

    //Read types:
    if (instanceTypes != null && instanceTypes._1.equals("tsv")) {
      LOG.info("Reading types (tsv format)...")
      val uriNotFound = HashSet[String]()

      var linesIterator : Iterator[String] = Iterator.empty
      try {
        linesIterator = Source.fromInputStream(instanceTypes._2, "UTF-8").getLines
      } catch {
        case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(instanceTypes._2).getLines
      }

      for (line <- linesIterator) {
          val Array(uri: String, typeURI: String) = line.trim().split('\t')

          try {
            resourceByURI(new DBpediaResource(uri).uri).types ::= OntologyType.fromURI(typeURI)
          } catch {
            case e: java.util.NoSuchElementException =>
              uriNotFound += uri
          }
      }
      LOG.warn("URI for %d type definitions not found!".format(uriNotFound.size) )
      LOG.info("Done.")
    } else if (instanceTypes != null && instanceTypes._1.equals("nt")) {
      LOG.info("Reading types (nt format)...")

      val uriNotFound = HashSet[String]()

      val redParser = new NxParser(instanceTypes._2)
      while (redParser.hasNext) {
        val triple = redParser.next
        val subj = triple(0).toString.replace(namespace, "")

        if (!subj.contains("__")) {
          val obj  = triple(2).toString.replace(namespace, "")

          try {
            if(!obj.endsWith("owl#Thing"))
              resourceByURI(new DBpediaResource(subj).uri).types ::= OntologyType.fromURI(obj)
          } catch {
            case e: java.util.NoSuchElementException =>
              uriNotFound += subj
          }
        }

      }
      LOG.warn("URI for %d type definitions not found!".format(uriNotFound.size) )
      LOG.info("Done.")
    }

    resourceByURI foreach {
      case (_, res) => resourceMap.put(res, res.support)
    }

    resourceMap
  }


  def fromPigFiles(
    wikipediaToDBpediaClosure: WikipediaToDBpediaClosure,
    counts: File,
    instanceTypes: File,
    namespace: String
  ): java.util.Map[DBpediaResource, Int] = fromPigInputStreams(
    wikipediaToDBpediaClosure,
    new FileInputStream(counts),
    if(instanceTypes == null)
      null
    else
      (
        if(instanceTypes.getName.endsWith("nt")) "nt" else "tsv",
        new FileInputStream(instanceTypes)
      ),
    namespace
  )


  def fromTSVFiles(
    conceptList: File,
    counts: File,
    instanceTypes: File
  ): java.util.Map[DBpediaResource, Int] = fromTSVInputStream(
    new FileInputStream(conceptList),
    new FileInputStream(counts),
    new FileInputStream(instanceTypes)
  )

}
