package org.dbpedia.spotlight.io

import io.Source
import java.io.{InputStream, FileInputStream, File}
import org.dbpedia.spotlight.model.SurfaceForm


/**
 * @author Joachim Daiber
 *
 */

//class SurfaceFormSource extends Iterator[SurfaceForm]


/**
 * Represents a source of SurfaceForms
 */

object SurfaceFormSource {

  def fromPigInputStream(in: InputStream): Iterator[SurfaceForm] =
    Source.fromInputStream(in).getLines() map {
      line: String => {
        val Array(ptitle, name, p_uri_sf, p_sf_uri, p_sf, wpid, c_uri) = line.trim().split('\t')
        val surfaceform = new SurfaceForm(name)
        surfaceform.support = 0
        surfaceform
      }
    }

  def fromPigFile(file: File): Iterator[SurfaceForm] = fromPigInputStream(new FileInputStream(file))


  def fromTSVInputStream(in: InputStream): Iterator[SurfaceForm] =
    Source.fromInputStream(in).getLines() map {
      line: String => {
        val name = line.trim()
        val surfaceform = new SurfaceForm(name)
        surfaceform.support = 0
        surfaceform
      }
    }

  def fromTSVFile(file: File): Iterator[SurfaceForm] = fromTSVInputStream(new FileInputStream(file))

}
