package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.db.disk.JDBMStore
import java.io.{FileInputStream, File}
import java.util.Map
import org.dbpedia.spotlight.db.Containers.SFContainer
import org.dbpedia.spotlight.model._

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class JDBMStoreIndexer(store: JDBMStore)
  extends SurfaceFormIndexer
  with ResourceIndexer
  with TokenIndexer
  with TokenOccurrenceIndexer
{

  store.create()

  //SURFACE FORMS

  def addSurfaceForm(sf: SurfaceForm, count: Int) {
    store.add(sf.name, Pair(sf.id, sf.support))
  }

  def addSurfaceForms(sfCount: Map[SurfaceForm, Int]) {
    sfCount.foreach{ case(sf, count) => addSurfaceForm(sf, count) }
    store.commit()
  }


  //DBPEDIA RESOURCES

  def addResource(resource: DBpediaResource, count: Int) {
    store.add(resource.id, resource)
  }

  def addResources(resourceCount: Map[DBpediaResource, Int]) {
    resourceCount.foreach{ case(resource, count) => addResource(res, count) }
    store.commit()
  }


  //TOKENS

  def addToken(token: Token, count: Int) {
    store.add(token.name, Pair(token.id, token.count))
  }

  def addTokens(tokenCount: Map[Token, Int]) {
    tokenCount.foreach{ case(token, count) => addToken(token, count) }
    store.commit()
  }


  //TOKEN OCCURRENCES

  def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Token, Int]) {
    store.add(resource.id, tokenCounts)
  }

  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Token, Int]]) {
    occs.foreach{ case(resource, tokenCounts) => addToken(token, tokenCounts) }
    store.commit()
  }




}

object JDBMStoreIndexer {

  def main(args: Array[String]) {
    val indexer = new JDBMStoreIndexer(new JDBMStore[String, Pair]("sf"))
    addSurfaceForms(sfStore, new FileInputStream(new File("/Volumes/Daten/DBpedia/Spotlight/surfaceForms-fromOccs-thresh10-TRD.set")))
  }

}