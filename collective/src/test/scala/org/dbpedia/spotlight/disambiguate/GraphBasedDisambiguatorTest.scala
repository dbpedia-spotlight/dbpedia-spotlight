package org.dbpedia.spotlight.disambiguate

import org.dbpedia.spotlight.disambiguate.GraphBasedDisambiguator
import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 5/31/12
 * Time: 10:41 PM
 */

class GraphBasedDisambiguatorTest{
  val disambiguator = new GraphBasedDisambiguator()

  @Test        // just try out test
  def testDisambiguatorName() {
      val name = "GraphBasedDisambiguatorRunner"
      println(disambiguator.name)
      assertEquals(name,disambiguator.name)
   }
}