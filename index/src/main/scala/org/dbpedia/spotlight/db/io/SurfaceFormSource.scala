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
    wikiClosure: WikipediaToDBpediaClosure = null
  ): Map[SurfaceForm, (Int, Int)] = {

    LOG.info("Creating SurfaceFormSource...")

    val sfMap = new HashMap[SurfaceForm, (Int, Int)]()

    LOG.info("Reading annotated and total counts...")

    var linesIterator : Iterator[String] = Iterator.empty
    try {
      linesIterator = Source.fromInputStream(sfAndTotalCounts, "UTF-8").getLines
    } catch {
      case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(sfAndTotalCounts).getLines
      }

    for (lineS <- linesIterator) {
        val line = lineS.trim().split('\t')

        val surfaceform = new SurfaceForm(line(0))
        val countAnnotated = line(1).toInt
        
        //Read the total count: If there is no total count for the
        //surface form, we use -1 to encode this case for handling 
        //in later steps.
        val countTotal = if( line.size == 3 ) line(2).toInt else -1

        sfMap.put(
          surfaceform,
          if(sfMap.get(surfaceform) != null) {
            val (existingCountAnnotated, existingCountTotal) = sfMap.get(surfaceform)
            (existingCountAnnotated + countAnnotated, existingCountTotal + countTotal)
          } else {
            (countAnnotated, countTotal)
          }
        )
    }

    LOG.info("Done.")

    sfMap
  }


  def fromPigFiles(
    sfAndTotalCounts: File,
    wikiClosure: WikipediaToDBpediaClosure = null
  ): Map[SurfaceForm, (Int, Int)] = {
    fromPigInputStreams(new FileInputStream(sfAndTotalCounts), wikiClosure)
  }


  def fromTSVInputStream(in: InputStream): Map[SurfaceForm, (Int, Int)] = {
    val sfFormMap = new HashMap[SurfaceForm, (Int, Int)]()

    var linesIterator : Iterator[String] = Iterator.empty
    try {
      linesIterator = Source.fromInputStream(in, "UTF-8").getLines
    } catch {
      case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(in).getLines
      }

    linesIterator map {
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
