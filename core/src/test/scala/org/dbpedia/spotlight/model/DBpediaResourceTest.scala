package org.dbpedia.spotlight.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers


/**
 * This ScalaTest test the construction and the full uri generation for a set of uri entries.
 *
 * @author Alexandre Cançado Cardoso - accardoso
 */


@RunWith(classOf[JUnitRunner])
class DBpediaResourceTest extends FlatSpec with ShouldMatchers {

  //Test Constructors from uri, type representation and toString

  "DBpediaResource[Berlin]" should "be construct when 'Berlin' is passed as uri" in {
      new DBpediaResource("Berlin").toString should be === "DBpediaResource[Berlin]"
  }

  it should "be construct when 'http://dbpedia.org/resource/Berlin' is passed as uri" in {
    new DBpediaResource("http://dbpedia.org/resource/Berlin").toString should be === "DBpediaResource[Berlin]"
  }

  "WiktionaryResource[http://en.wikipedia.org/resource/Berlin]" should "be construct when 'http://en.wikipedia.org/resource/Berlin' is passed as uri" in {
    new DBpediaResource("http://en.wikipedia.org/resource/Berlin").toString should be === "WiktionaryResource[http://en.wikipedia.org/resource/Berlin]"
  }

  "WiktionaryResource[http://pt.wikipedia.org/resource/Berlim]" should "be construct when 'http://pt.wikipedia.org/resource/Berlim' is passed as uri" in {
    new DBpediaResource("http://pt.wikipedia.org/resource/Berlim").toString should be === "WiktionaryResource[http://pt.wikipedia.org/resource/Berlim]"
  }

  "WiktionaryResource[http://en.wikipedia.org/resource/Berlim]" should "be construct when 'http://en.wikipedia.org/resource/Berlim' is passed as uri" in {
    new DBpediaResource("http://en.wikipedia.org/resource/Berlim").toString should be === "WiktionaryResource[http://en.wikipedia.org/resource/Berlim]"
  }

  "WiktionaryResource[http://pt.wikipedia.org/resource/Berlin]" should "be construct when 'http://pt.wikipERROedia.org/resource/Berlin' is passed as uri" in {
    new DBpediaResource("http://pt.wikipedia.org/resource/Berlin").toString should be === "WiktionaryResource[http://pt.wikipedia.org/resource/Berlin]"
  }

  "WiktionaryResource[http://ontologia.globo.com/resource/Berlim]" should "be construct when 'http://ontologia.globo.com/resource/Berlim' is passed as uri" in {
    new DBpediaResource("http://ontologia.globo.com/resource/Berlim").toString should be === "WiktionaryResource[http://ontologia.globo.com/resource/Berlim]"
  }


  //Test full uri generators and getFullUri

  "The full uri http://dbpedia.org/resource/Berlin" should "be generated for uri='Berlin'" in {
    new DBpediaResource("Berlin").getFullUri should be === List(SpotlightConfiguration.DEFAULT_NAMESPACE, "Berlin").mkString
  }

  it should "be generated for uri='http://dbpedia.org/resource/Berlin'" in {
    new DBpediaResource("http://dbpedia.org/resource/Berlin").getFullUri should be === "http://dbpedia.org/resource/Berlin"
  }

  "The full uri http://en.wikipedia.org/resource/Berlin" should "be generated for uri='http://en.wikipedia.org/resource/Berlin'" in {
    new DBpediaResource("http://en.wikipedia.org/resource/Berlin").getFullUri should be === "http://en.wikipedia.org/resource/Berlin"
  }

  "The full uri http://pt.wikipedia.org/resource/Berlim" should "be generated for uri='http://pt.wikipedia.org/resource/Berlim'" in {
    new DBpediaResource("http://pt.wikipedia.org/resource/Berlim").getFullUri should be === "http://pt.wikipedia.org/resource/Berlim"
  }

  "The full uri http://en.wikipERROedia.org/resource/Berlim" should "be generated for uri='http://en.wikipERROedia.org/resource/Berlim'" in {
    new DBpediaResource("http://en.wikipERROedia.org/resource/Berlim").getFullUri should be === "http://en.wikipERROedia.org/resource/Berlim"
  }

  "The full uri http://ontologia.globo.com/resource/Berlim" should "be generated for uri='http://ontologia.globo.com/resource/Berlim'" in {
    new DBpediaResource("http://ontologia.globo.com/resource/Berlim").getFullUri should be === "http://ontologia.globo.com/resource/Berlim"
  }

}