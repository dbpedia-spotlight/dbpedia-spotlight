package org.dbpedia.spotlight.db.io

import io.Source
import java.io.{InputStream, FileInputStream, File}
import org.dbpedia.spotlight.model.SurfaceForm
import scala.Array
import java.util.zip.GZIPInputStream
import java.util.{Map, HashMap}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream


/**
 * Represents a source of SurfaceForms
 */

object SurfaceFormSource {

  def fromPigInputStream(in: InputStream): Map[SurfaceForm, Int] = {

    val sfFormMap = new HashMap[SurfaceForm, Int]()

    Source.fromInputStream(in).getLines() map {
      line: String => {
        val Array(name, count) = line.trim().split('\t')
        val surfaceform = new SurfaceForm(name)

        val c = sfFormMap.get(surfaceform) match {
          case c: Int => c
          case _ => 0
        }

        sfFormMap.put(surfaceform, c + count.toInt)
      }
    }

    sfFormMap
  }


  def fromPigFile(file: File): Map[SurfaceForm, Int] = {
    if (file.getName.endsWith("gz"))
      fromPigInputStream(new GZIPInputStream(new FileInputStream(file)))
    else
      fromPigInputStream(new FileInputStream(file))

  }


  def fromTSVInputStream(in: InputStream): Map[SurfaceForm, Int] = {
    val sfFormMap = new HashMap[SurfaceForm, Int]()

    Source.fromInputStream(in).getLines() map {
      line: String => {
        val name = line.trim()
        sfFormMap.put(new SurfaceForm(name), 0)
      }
    }

    sfFormMap
  }

  def fromTSVFile(file: File): Map[SurfaceForm, Int] = {
    if (file.getName.endsWith("gz"))
      fromTSVInputStream(new GZIPInputStream(new FileInputStream(file)))
    else if (file.getName.endsWith("bz2"))
      fromTSVInputStream(new BZip2CompressorInputStream(new FileInputStream(file)))
    else
      fromTSVInputStream(new FileInputStream(file))
  }


}
