package org.dbpedia.spotlight.disambiguate

import junit.framework.TestCase
import org.junit.Test
import junit.framework.Assert.assertEquals


/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 5/31/12
 * Time: 10:41 PM
 */


class GraphBasedDisambiguatorTest extends TestCase{
  val disambiguator = new GraphBasedDisambiguator()

  @Test
  def testDisambiguatorName() {
      val name = "GraphBasedDisambiguatorRunner"
      assertEquals(disambiguator.name,name)
   }

}
