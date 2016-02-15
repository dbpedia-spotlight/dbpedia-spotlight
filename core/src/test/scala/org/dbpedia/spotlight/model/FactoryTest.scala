package org.dbpedia.spotlight.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

/**
 * The unit tests for Factory class.
 *
 * @author Alexandre Cançado Cardoso - accardoso
 */

@RunWith(classOf[JUnitRunner])
class FactoryTest extends FlatSpec with ShouldMatchers {

  /* Test Factory.SurfaceForm */
  "The correct Surface Form" should "be created for the surface form string 'Berlin'" in {
    val surfaceFormString = "Berlin"
    Factory.SurfaceForm.fromString(surfaceFormString).name should be === (new SurfaceForm(surfaceFormString)).name
  }

  it should "be created for the uri 'http://dbpedia.org/resource/Berlin'" in {
    val uriString = "http://dbpedia.org/resource/Berlin"
    Factory.SurfaceForm.fromDBpediaResourceURI(uriString, false).name should be === (new SurfaceForm(uriString)).name
  }

  it should "be created for the uri 'Berlin'" in {
    val uriString = "Berlin"
    Factory.SurfaceForm.fromDBpediaResourceURI(uriString, true).name should be === (new SurfaceForm(uriString)).name.toLowerCase
  }

  it should "be created for 'DBpediaResource[Berlin]'" in {
    val uriString = "Berlin"
    val uri = new DBpediaResource("Berlin")
    Factory.SurfaceForm.fromDBpediaResourceURI(uri, false).name should be === (new SurfaceForm(uriString)).name
  }

  it should "be created for the full uri of 'DBpediaResource[Berlin]'" in {
    val uriString = "Berlin"
    val uri = new DBpediaResource("Berlin")
    Factory.SurfaceForm.fromDBpediaResourceURI(uri.uri, false).name should be === (new SurfaceForm(uriString)).name
  }

  it should "be created for the url 'http://en.wikipedia.org/resource/Berlin'" in {
    val surfaceFormString = "Berlin"
    val url = "http://en.wikipedia.org/resource/Berlin"
    Factory.SurfaceForm.fromWikiPageTitle(FactoryTest.extractPageTitleFrom(url), false).name should be === (new SurfaceForm(surfaceFormString)).name
  }

  /* Test Factory.SurfaceFormOccurrence */

  it should "todo" in {
    val uriString = "http://dbpedia.org/resource/Berlin"
    val sf = new SurfaceForm("Berlin")
    val context = new Text("Berlin is a capital.")
    val offset = 1
    val inputDBpediaResourceOcc = new DBpediaResourceOccurrence(new DBpediaResource(uriString), sf, context, offset)
    val expectedSFOcc = new SurfaceFormOccurrence(sf, context, offset)
    Factory.SurfaceFormOccurrence.from(inputDBpediaResourceOcc).equals(expectedSFOcc) should be === true
  }

  /* Test Factory.DBpediaResourceOccurrence */
  //This already existing test are implemented at before existing class FactoryTests

}

object FactoryTest {

  def extractPageTitleFrom(url: String): String = {
    url.substring(url.lastIndexOf('/')+1)
  }

}