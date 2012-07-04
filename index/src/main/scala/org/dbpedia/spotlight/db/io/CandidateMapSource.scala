package org.dbpedia.spotlight.db.io

import io.Source
import scala.Predef._
import org.dbpedia.spotlight.model._
import java.io.{File, FileInputStream, InputStream}
import org.dbpedia.spotlight.db.model.{SurfaceFormStore, ResourceStore}
import java.lang.String
import collection.mutable.HashSet
import org.dbpedia.spotlight.db.WikipediaToDBpediaClosure
import scala.Int
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.exceptions._


/**
 * @author Joachim Daiber
 *
 */

/**
 * Represents a source of a SF->Res. mapping
 */

object CandidateMapSource {

  private val LOG = LogFactory.getLog(this.getClass)

  def fromPigInputStreams(
    pairCounts: InputStream,
    wikipediaToDBpediaClosure: WikipediaToDBpediaClosure,
    resStore: ResourceStore,
    sfStore: SurfaceFormStore
  ): java.util.Map[Pair[Int, Int], Int] = {

    val candidateMap = new java.util.HashMap[Pair[Int, Int], Int]()

    var uriNotFound = 0
    var sfNotFound  = 0
    var uriIgnored  = 0

    LOG.info("Reading Candidate Map.")
    Source.fromInputStream(pairCounts).getLines() foreach {
      line: String => {
        try {
          val Array(sf, wikiurl, count) = line.trim().split('\t')
          val uri = wikipediaToDBpediaClosure.wikipediaToDBpediaURI(wikiurl)

          val c = Pair(sfStore.getSurfaceForm(sf).id, resStore.getResourceByName(uri).id)
          val initialCount = candidateMap.get(c) match {
            case c: Int => c
            case _ => 0
          }

          candidateMap.put(c, initialCount + count.toInt)
        } catch {
          case e: NotADBpediaResourceException     => uriIgnored += 1
          case e: ArrayIndexOutOfBoundsException   => LOG.warn("WARNING: Could not read line.")
          case e: DBpediaResourceNotFoundException => {uriNotFound += 1; println(line)}
          case e: SurfaceFormNotFoundException     => sfNotFound += 1
        }
      }
    }
    LOG.info("Done.")


    LOG.warn("DBpedia resource not found: %d".format(uriNotFound) )
    LOG.warn("Invalid DBpedia resources (e.g. disambiguation page): %d".format(uriIgnored) )
    LOG.warn("SF not found: %d".format(sfNotFound) )

    candidateMap
  }

  def fromPigFiles(
    pairCounts: File,
    wikipediaToDBPediaClosure: WikipediaToDBpediaClosure,
    resStore: ResourceStore,
    sfStore: SurfaceFormStore
  ): java.util.Map[Pair[Int, Int], Int] = fromPigInputStreams(new FileInputStream(pairCounts), wikipediaToDBPediaClosure, resStore, sfStore)


  def fromTSVInputStream(
    candmap: InputStream,
    resourceStore: ResourceStore,
    surfaceFormStore: SurfaceFormStore
  ): java.util.Map[Candidate, Int] = {

    val candidateMap = new java.util.HashMap[Candidate, Int]()

    val uriNotFound = HashSet[String]()
    val sfNotFound  = HashSet[String]()

    Source.fromInputStream(candmap).getLines() foreach {
      line: String => {
        try {
          val s1 = line.trim().split("\t")
          val s2 = s1(0).split(" ")
          val sf = s1(1)
          val count = s2(0)
          val uri = new DBpediaResource(s2(1)).uri

          candidateMap.put(
            Candidate(surfaceFormStore.getSurfaceForm(sf), resourceStore.getResourceByName(uri)),
            count.toInt
          )
        } catch {
          case e: ArrayIndexOutOfBoundsException => LOG.warn("Could not read line.")
          case e: DBpediaResourceNotFoundException => println(line)
          case e: SurfaceFormNotFoundException => sfNotFound += line
        }
      }
    }

    LOG.warn("URI for %d candidate definitions not found!".format(uriNotFound.size) )
    LOG.warn("SF for %d candidate definitions not found!".format(sfNotFound.size) )

    candidateMap
  }

  def fromTSVFile(
    candmap: File,
    resStore: ResourceStore,
    sfStore: SurfaceFormStore
    ): java.util.Map[Candidate, Int] = {
    fromTSVInputStream(new FileInputStream(candmap), resStore, sfStore)
  }

}
