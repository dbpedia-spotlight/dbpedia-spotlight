package org.dbpedia.spotlight.model


import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: pablo
 * Date: 4/13/11
 * Time: 1:42 PM
 * To change this template use File | Settings | File Templates.
 */
class FactoryTests {

  @Test
  def idStringToDBpediaResource() {
    val examples = List("Germany", "Apple");

     examples.foreach( dbpediaID => {
      val dBpediaResource: DBpediaResource = Factory.DBpediaResource.from(dbpediaID)
      assert(dBpediaResource.uri.equals(dbpediaID))
      assert(dBpediaResource.getTypes.size() > 0)
      assert(dBpediaResource.support > 0)
    })

  }

  @Test
  def uriToSurfaceForm() {
    val examples = Map("Huge"->"Huge",
      "Huge_(TV_series)"->"Huge",
      "Huge_cardinal"->"Huge cardinal",
      "Apple_(disambiguation)"->"Apple",
      "Apple_%28disambiguation%29"->"Apple");

    examples.keys.foreach( title => {
      val r = new DBpediaResource(title)
      val s = Factory.createSurfaceFormFromDBpediaResourceURI(r, false)
      printf("%-30s=%30s \n",title,r.uri)
      printf("%-30s=%30s \n",examples(title),s.name)
      assert(s.name.equals(examples(title)))
    })
  }


}