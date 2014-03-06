package org.dbpedia.spotlight.db.io

import io.Source
import java.io.{File, FileInputStream, InputStream}
import org.dbpedia.spotlight.model.Factory.OntologyType
import scala.collection.JavaConverters._
import java.util.NoSuchElementException
import scala.collection.mutable.HashSet
import org.dbpedia.spotlight.db.WikipediaToDBpediaClosure
import org.dbpedia.spotlight.log.SpotlightLog
import scala.Predef._
import scala.Array
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.exceptions.NotADBpediaResourceException
import org.semanticweb.yars.nx.parser.NxParser

import org.dbpedia.extraction.util.WikiUtil


/**
 * Represents a source of DBpediaResources.
 *
 * Type definitions must be prepared beforehand, see
 *  src/main/scripts/types.sh
 *
 * @author Joachim Daiber
 */

object DBpediaResourceSource {

  def fromTSVInputStream(
    conceptList: InputStream,
    counts: InputStream,
    instanceTypes: InputStream
    ): java.util.Map[DBpediaResource, Int] = {

    var id = 0

    //The list of concepts may contain non-unique elements, hence convert it to a Set first to make sure
    //we do not count elements more than once.
    val resourceMap: Map[String, DBpediaResource] = (Source.fromInputStream(conceptList).getLines().toSet map {
      line: String => {
        val res = new DBpediaResource(line.trim)
        res.id = id
        id += 1
        Pair(res.uri, res)
      }
    }).toMap

    //Read counts:
    Source.fromInputStream(counts).getLines() foreach {
      line: String => {
        val Array(uri: String, count: String) = line.trim().split('\t')
        resourceMap(new DBpediaResource(uri).uri).setSupport(count.toInt)
      }
    }

    //Read types:
    val uriNotFound = HashSet[String]()
    Source.fromInputStream(instanceTypes).getLines() foreach {
      line: String => {
        val Array(id: String, typeURI: String) = line.trim().split('\t')

        try {
          resourceMap(new DBpediaResource(id).uri).types ::= OntologyType.fromURI(typeURI)
        } catch {
          case e: NoSuchElementException =>
            //System.err.println("WARNING: DBpedia resource not in concept list %s (%s)".format(id, typeURI) )
            uriNotFound += id
        }
      }
    }
    SpotlightLog.warn(this.getClass, "URI for %d type definitions not found!", uriNotFound.size)

    resourceMap.iterator.map( f => Pair(f._2, f._2.support) ).toMap.asJava
  }


  /**
   * Normalize the URI resulting from Pig to a format useable for us.
   * At the moment, the Pig URIs are in DBpedia format but double-encoded.
   *
   * @param uri the DBpedia URI returned by Pig
   * @return
   */
  def normalizePigURI(uri: String) = {
    try {
      //This seems a bit over the top (this is necessary because of the format of the data as it comes from pignlproc):
      WikiUtil.wikiEncode(WikiUtil.wikiDecode(WikiUtil.wikiDecode(uri)))
    } catch {
      case e: Exception => println("Conversion to correct URI format failed at %s".format(uri)); uri
    }

  }


  def fromPigInputStreams(
    wikipediaToDBpediaClosure: WikipediaToDBpediaClosure,
    resourceCounts: InputStream,
    instanceTypes: (String, InputStream),
    namespace: String
  ): java.util.Map[DBpediaResource, Int] = {

    SpotlightLog.info(this.getClass, "Creating DBepdiaResourceSource.")

    var id = 1

    val resourceMap = new java.util.HashMap[DBpediaResource, Int]()
    val resourceByURI = scala.collection.mutable.HashMap[String, DBpediaResource]()

    SpotlightLog.info(this.getClass, "Reading resources+counts...")

    Source.fromInputStream(resourceCounts).getLines() foreach {
      line: String => {
        try {
          val Array(wikiurl, count) = line.trim().split('\t')
          val res = new DBpediaResource(wikipediaToDBpediaClosure.wikipediaToDBpediaURI(normalizePigURI(wikiurl)))

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
          case e: scala.MatchError => //Ignore lines with multiple tabs
        }

      }
    }

    //Read types:
    if (instanceTypes != null && instanceTypes._1.equals("tsv")) {
      SpotlightLog.info(this.getClass, "Reading types (tsv format)...")
      val uriNotFound = HashSet[String]()
      Source.fromInputStream(instanceTypes._2).getLines() foreach {
        line: String => {
          val Array(uri: String, typeURI: String) = line.trim().split('\t')

          try {
            resourceByURI(new DBpediaResource(uri).uri).types ::= OntologyType.fromURI(typeURI)
          } catch {
            case e: java.util.NoSuchElementException =>
              uriNotFound += uri
          }
        }
      }
      SpotlightLog.warn(this.getClass, "URI for %d type definitions not found!".format(uriNotFound.size) )
      SpotlightLog.info(this.getClass, "Done.")
    } else if (instanceTypes != null && instanceTypes._1.equals("nt")) {
      SpotlightLog.info(this.getClass, "Reading types (nt format)...")

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
      SpotlightLog.info(this.getClass, "URI for %d type definitions not found!".format(uriNotFound.size) )
      SpotlightLog.info(this.getClass, "Done.")
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
