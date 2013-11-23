package org.dbpedia.spotlight.db.io

import io.Source
import org.dbpedia.spotlight.model.SurfaceForm
import scala.Array
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.dbpedia.spotlight.log.SpotlightLog
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

  def fromPigInputStreams(
    sfAndTotalCounts: InputStream,
    wikiClosure: WikipediaToDBpediaClosure = null
  ): Map[SurfaceForm, (Int, Int)] = {

    SpotlightLog.info(this.getClass, "Creating SurfaceFormSource...")

    val sfMap = new HashMap[SurfaceForm, (Int, Int)]()

    SpotlightLog.info(this.getClass, "Reading annotated and total counts...")
    Source.fromInputStream(sfAndTotalCounts).getLines() foreach {
      lineS: String => {
        val line = lineS.trim().split('\t')

        val surfaceform = new SurfaceForm(line(0))
        val countAnnotated = line(1).toInt

        //Read only surface forms whose annotated count is not -1 (-1 is used to indicate lowercase counts)
        if (countAnnotated != -1) {

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
      }
    }

    SpotlightLog.info(this.getClass, "Done.")

    sfMap
  }

  def lowercaseCountsFromPigInputStream(sfAndTotalCounts: InputStream): Map[String, Int] = {

    SpotlightLog.info(this.getClass, "Determining lowercase surfaceform counts...")

    val lowercaseCountsMap = new HashMap[String, Int]()

    Source.fromInputStream(sfAndTotalCounts).getLines() foreach {
      lineS: String => {
        val line = lineS.trim().split('\t')

        //Lowercase counts have a "-1" annotated count:
        if (line(1).equals("-1")) {
          val surfaceform = line(0)
          val countTotal = if( line.size == 3 ) line(2).toInt else -1
          lowercaseCountsMap.put(surfaceform, countTotal)
        }
      }
    }

    SpotlightLog.info(this.getClass, "Done.")
    lowercaseCountsMap
  }


  def fromPigFiles(
    sfAndTotalCounts: File,
    wikiClosure: WikipediaToDBpediaClosure = null
  ): Map[SurfaceForm, (Int, Int)] = {
    fromPigInputStreams(new FileInputStream(sfAndTotalCounts), wikiClosure)
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
