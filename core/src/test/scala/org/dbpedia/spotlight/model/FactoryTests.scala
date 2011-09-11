package org.dbpedia.spotlight.model


import org.dbpedia.spotlight.exceptions.ItemNotFoundException
import io.Source
import org.junit.{Before, Test}

class FactoryTests {

    val spotlightFactory: SpotlightFactory = new SpotlightFactory(new SpotlightConfiguration("conf/server.properties"))

    @Test
    def dbpediaResources() {
        val examples = List("Germany", "Apple", "Train", "Giant_oil_and_gas_fields");
        //Source.fromFile("/Users/jodaiber/Desktop/conceptURIs.list", "UTF-8").getLines().take(100)

        examples.foreach( dbpediaID => {
            val dBpediaResource: DBpediaResource = spotlightFactory.DBpediaResource.from(dbpediaID)
            assert(dBpediaResource.uri.equals(dbpediaID))
            assert(dBpediaResource.getTypes.size() >= 0)
            assert(dBpediaResource.support >= 0)
            assert(!dBpediaResource.getTypes.contains(null))
        })

    }

    @Test
    def dbpediaResourcesNotThere() {
        try {
            val dBpediaResource: DBpediaResource = spotlightFactory.DBpediaResource.from("TotallyUnknownID")
        } catch {
            case e: ItemNotFoundException =>  
        }
    }


    @Test
    def specificDBpediaResource() {
        val dBpediaResource: DBpediaResource = spotlightFactory.DBpediaResource.from("Berlin")
        assert(dBpediaResource.getTypes.contains(Factory.OntologyType.fromQName("Freebase:/location/location")))
        assert(dBpediaResource.getTypes.contains(Factory.OntologyType.fromQName("DBpedia:City")))
        assert(dBpediaResource.getTypes.contains(Factory.OntologyType.fromQName("Schema:City")))

    }

    @Test
    def specificDBpediaResourceWithEmptyTypes() {
        val dBpediaResource: DBpediaResource = spotlightFactory.DBpediaResource.from("Giant_oil_and_gas_fields")
        assert(dBpediaResource.getTypes.size() == 0)
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