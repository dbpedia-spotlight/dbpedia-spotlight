package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.db.disk.JDBMStore
import org.dbpedia.spotlight.model.SurfaceForm
import io.Source
import java.io.{FileInputStream, File, InputStream}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object JDBMStoreIndexer {

  def addSurfaceForms(store: JDBMStore[String, SurfaceForm], input: InputStream) {
    store.create()

    Source.fromInputStream(input) getLines() foreach {
      line: String => {
        val Array(ptitle, name, p_uri_sf, p_sf_uri, p_sf, wpid, c_uri) = line.trim().split('\t')
        val surfaceform = new SurfaceForm(name)
        surfaceform.p = p_sf.toFloat

        store.add(name, surfaceform)
      }
    }

    store.commit()
  }

  def main(args: Array[String]) {

    val sfStore = new JDBMStore[String, SurfaceForm]("sf")
    addSurfaceForms(sfStore, new FileInputStream(new File("/Users/jodaiber/Desktop/nerd_stats_enwiki_20120502_5grams.sorted_by_psf.tsv")))

  }

}