package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.db.disk.JDBMStore
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.db.Containers.SFContainer
import org.dbpedia.spotlight.model.{DBpediaResource, ResourceIndexer, SurfaceForm, SurfaceFormIndexer}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class JDBMStoreIndexer(store: JDBMStore)
  extends SurfaceFormIndexer
  with ResourceIndexer {

  store.create()

  //SURFACE FORMS

  def add(sf: SurfaceForm, count: Int) {
    store.add(sf.name, new SFContainer(sf.id, sf.support))
  }

  def add(sfCount: Map[SurfaceForm, Int]) {
    sfCount.foreach{ case(sf, count) => add(sf, count) }
    store.commit()
  }


  //DBPEDIA RESOURCES

  def add(resource: DBpediaResource, count: Int)
  def add(resourceCount: Map[DBpediaResource, Int])



}

object JDBMStoreIndexer {

  def main(args: Array[String]) {
    val indexer = new JDBMStoreIndexer(new JDBMStore[String, SFContainer]("sf"))
    addSurfaceForms(sfStore, new FileInputStream(new File("/Volumes/Daten/DBpedia/Spotlight/surfaceForms-fromOccs-thresh10-TRD.set")))
  }

}