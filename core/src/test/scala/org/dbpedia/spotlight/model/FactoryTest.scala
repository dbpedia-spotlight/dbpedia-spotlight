package org.dbpedia.spotlight.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.dbpedia.spotlight.lucene.search.BaseSearcher
import org.dbpedia.spotlight.lucene.LuceneManager

import java.io.File
import org.apache.lucene.index.IndexReader
import org.apache.lucene.document.Document
import org.dbpedia.spotlight.model.Factory

//import org.dbpedia.spotlight.util.IndexingConfiguration
import org.apache.lucene.store.FSDirectory

/**
 *
 * @author Alexandre Cançado Cardoso - accardoso
 */

@RunWith(classOf[JUnitRunner])
class FactoryTest extends FlatSpec with ShouldMatchers {

  /* Test Factory.SurfaceForm */

  "The correct Surface Form" should "be created for the surface form string 'Berlin'" in {
    val surfaceFormString = "Berlin"
    Factory.SurfaceForm.fromString(surfaceFormString).equals(new SurfaceForm(surfaceFormString)) should be === true
  }

  it should "be created for the uri 'http://dbpedia.org/resource/Berlin'" in {
    val uriString = "http://dbpedia.org/resource/Berlin"
    Factory.SurfaceForm.fromDBpediaResourceURI(uriString, false).equals(new SurfaceForm(uriString)) should be === true
  }

  it should "be created for the uri 'Berlin'" in {
    val uriString = "Berlin"
    Factory.SurfaceForm.fromDBpediaResourceURI(uriString, false).equals(new SurfaceForm(uriString)) should be === true
  }

  it should "be created for 'DBpediaResource[Berlin]'" in {
    val uriString = "Berlin"
    val uri = new DBpediaResource("Berlin")
    Factory.SurfaceForm.fromDBpediaResourceURI(uri, false).equals(new SurfaceForm(uriString)) should be === true
  }

  it should "be created for the full uri of 'DBpediaResource[Berlin]'" in {
    val uriString = "Berlin"
    val uri = new DBpediaResource("Berlin")
    Factory.SurfaceForm.fromDBpediaResourceURI(uri.uri, false).equals(new SurfaceForm(uriString)) should be === true
  }

  it should "be created for the url 'http://en.wikipedia.org/resource/Berlin'" in {
    val surfaceFormString = "Berlin"
    val url = "http://en.wikipedia.org/resource/Berlin"
    Factory.SurfaceForm.fromWikiPageTitle(url, false).equals(new SurfaceForm(surfaceFormString)) should be === true
  }

  it should "be created for the url 'http://pt.wikipedia.org/resource/Berlim'" in {
    val surfaceFormString = "Berlim"
    val url = "http://pt.wikipedia.org/resource/Berlim"
    Factory.SurfaceForm.fromWikiPageTitle(url, false).equals(new SurfaceForm(surfaceFormString)) should be === true
  }

  it should "be created for the url 'http://en.wikipedia.org/resource/Berlim'" in {
    val surfaceFormString = "Berlim"
    val url = "http://en.wikipedia.org/resource/Berlim"
    Factory.SurfaceForm.fromWikiPageTitle(url, false).equals(new SurfaceForm(surfaceFormString)) should be === true
  }

//  it should "be created for the url 'http://ontologia.globo.com/resource/Berlim'" in {
//    val surfaceFormString = "Berlim"
//    val url = "http://ontologia.globo.com/resource/Berlim"
//    Factory.SurfaceForm.fromWikiPageTitle(url, false).equals(new SurfaceForm(surfaceFormString)) should be === true
//  }


  /* Test Factory.DBpediaResourceOccurrence */



}