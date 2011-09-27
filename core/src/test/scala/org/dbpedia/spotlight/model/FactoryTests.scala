package org.dbpedia.spotlight.model


import org.dbpedia.spotlight.exceptions.ItemNotFoundException
import io.Source
import org.junit.{Before, Test}
import org.junit.Assert._

/**
 * For methods that create model objects, making sure factory works.
 *
 * @author pablomendes, Joachim Daiber
 */
class FactoryTests {

    /**
     * Makes sure that types are created correct from qNames and from URIs
     * Freebase types can have one or two levels
     */
    @Test
    def createFreebaseType() {
        assert(Factory.OntologyType.fromQName("Freebase:/location").equals(Factory.OntologyType.fromURI("http://rdf.freebase.com/ns/location")))
        assert(Factory.OntologyType.fromQName("Freebase:/education/university").equals(Factory.OntologyType.fromURI("http://rdf.freebase.com/ns/education/university")))
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

    @Test
    def resOccToSFOcc() {
        val examples = List("Germany", "Apple", "Train", "Giant_oil_and_gas_fields");
        //Source.fromFile("/Users/jodaiber/Desktop/conceptURIs.list", "UTF-8").getLines().take(100)
        val r = new DBpediaResource("Test")
        val sf = new SurfaceForm("test")
        val t = new Text("This is a test");
        val resOcc = new DBpediaResourceOccurrence("paragraph1",r,sf,t,10)
        val sfOcc = new SurfaceFormOccurrence(sf,t,10)
        val convertedSfOcc = Factory.SurfaceFormOccurrence.from(resOcc)
        assertEquals(sfOcc,convertedSfOcc)
        assert(sfOcc.hashCode == convertedSfOcc.hashCode)
        /*
        val id : String,
                                val resource : DBpediaResource,
                                val surfaceForm : SurfaceForm,
                                val context : Text,
                                val textOffset : Int,
                                val provenance : Provenance.Value = Provenance.Undefined,
                                var similarityScore : Double = -1,
                                var percentageOfSecondRank : Double = -1,
                                var contextualScore: Double = -1
         */

    }

}