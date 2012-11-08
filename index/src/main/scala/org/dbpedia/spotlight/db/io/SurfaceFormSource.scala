package org.dbpedia.spotlight.db.io

import io.Source
import org.dbpedia.spotlight.model.SurfaceForm
import scala.Array
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.logging.LogFactory
import java.io.{File, InputStream, FileInputStream}
import scala.Predef._
import java.util.{Map, HashMap}
import scala.collection.JavaConversions._

/**
 * Represents a source of SurfaceForms
 */

object SurfaceFormSource {

  private val LOG = LogFactory.getLog(this.getClass)

  def fromPigInputStreams(sfCounts: InputStream, phraseCounts: InputStream): Map[SurfaceForm, (Int, Int)] = {

    LOG.info("Creating SurfaceFormSource...")

    val annotatedCounts = new HashMap[SurfaceForm, Int]()
    val totalCounts = new HashMap[SurfaceForm, Int]()

    LOG.info("Reading annotated counts...")
    Source.fromInputStream(sfCounts).getLines() foreach {
      line: String => {
        val Array(name, count) = line.trim().split('\t')
        val surfaceform = new SurfaceForm(name)

        val c = annotatedCounts.get(surfaceform) match {
          case c: Int => c
          case _ => 0
        }

        annotatedCounts.put(surfaceform, c + count.toInt)
      }
    }

    LOG.info("Reading phrase counts...")
    Source.fromInputStream(phraseCounts).getLines() foreach {
      line: String => {
        val Array(name, count) = line.trim().split('\t')
        val surfaceform = new SurfaceForm(name)

        if(annotatedCounts.containsKey(surfaceform)) {

          val c = totalCounts.get(surfaceform) match {
            case c: Int => c
            case _ => 0
          }

          totalCounts.put(surfaceform, c + count.toInt)
        }
      }
    }

    LOG.info("Done.")

    val sfMap = new HashMap[SurfaceForm, (Int, Int)]()

    annotatedCounts.keySet foreach {
      sf: SurfaceForm => {
        val totalCount = totalCounts.get(sf) match {
          case c: Int => c
          case _ => 0
        }
        sfMap.put(sf, Pair(annotatedCounts.get(sf), totalCount))
      }
    }

    sfMap
  }


  def fromPigFiles(sfCounts: File, totalCounts: File): Map[SurfaceForm, (Int, Int)] = {
      fromPigInputStreams(new FileInputStream(sfCounts), new FileInputStream(totalCounts))
  }


  def fromTSVInputStream(in: InputStream): Map[SurfaceForm, (Int, Int)] = {
    val sfFormMap = new HashMap[SurfaceForm, (Int, Int)]()

    Source.fromInputStream(in).getLines() map {
      line: String => {
        val name = line.trim()
        sfFormMap.put(new SurfaceForm(name), (0, 0))
      }
    }

    sfFormMap
  }

  def fromTSVFile(file: File): Map[SurfaceForm, (Int, Int)] = {
    if (file.getName.endsWith("gz"))
      fromTSVInputStream(new GZIPInputStream(new FileInputStream(file)))
    else if (file.getName.endsWith("bz2"))
      fromTSVInputStream(new BZip2CompressorInputStream(new FileInputStream(file), true))
    else
      fromTSVInputStream(new FileInputStream(file))
  }


}
