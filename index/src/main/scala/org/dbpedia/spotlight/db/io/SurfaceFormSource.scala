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
import org.dbpedia.spotlight.db.WikipediaToDBpediaClosure
import org.dbpedia.spotlight.db.memory.MemoryResourceStore
import org.dbpedia.extraction.util.WikiUtil

/**
 * Represents a source of SurfaceForms
 *
 * @author Joachim Daiber
 */

object SurfaceFormSource {

  private val LOG = LogFactory.getLog(this.getClass)

  def fromPigInputStreams(
    sfAndTotalCounts: InputStream,
    wikiClosure: WikipediaToDBpediaClosure = null,
    resStore: MemoryResourceStore = null
  ): Map[SurfaceForm, (Int, Int)] = {

    LOG.info("Creating SurfaceFormSource...")

    val sfMap = new HashMap[SurfaceForm, (Int, Int)]()

    LOG.info("Reading annotated and total counts...")
    Source.fromInputStream(sfAndTotalCounts).getLines() foreach {
      lineS: String => {
        val line = lineS.trim().split('\t')

        val surfaceform = new SurfaceForm(line(0))
        val countAnnotated = line(1).toInt
        val countTotal = if( line.size == 3 ) line(2).toInt else -1

        sfMap.put(surfaceform, (countAnnotated, countTotal))
      }
    }

    LOG.info("Done.")

    sfMap
  }


  def fromPigFiles(
    sfAndTotalCounts: File,
    wikiClosure: WikipediaToDBpediaClosure = null,
    resStore: MemoryResourceStore  = null
  ): Map[SurfaceForm, (Int, Int)] = {
    fromPigInputStreams(new FileInputStream(sfAndTotalCounts), wikiClosure, resStore)
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
      fromTSVInputStream(new BZip2CompressorInputStream(new FileInputStream(file)))
    else
      fromTSVInputStream(new FileInputStream(file))
  }


}
