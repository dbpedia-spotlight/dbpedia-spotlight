//package org.dbpedia.spotlight.model
//
//
//import org.junit.Test
//
//class FactoryTests {
//
//    @Test
//    def dbpediaResources() {
//        val examples = List("Germany", "Apple", "Train", "Giant_oil_and_gas_fields");
//
//        examples.foreach( dbpediaID => {
//            val dBpediaResource: DBpediaResource = Factory.DBpediaResource.from(dbpediaID)
//            assert(dBpediaResource.uri.equals(dbpediaID))
//            assert(dBpediaResource.getTypes.size() >= 0)
//            assert(dBpediaResource.support > 0)
//        })
//
//    }
//
//    @Test
//    def specificDBpediaResource() {
//        val dBpediaResource: DBpediaResource = Factory.DBpediaResource.from("Berlin")
//        assert(dBpediaResource.getTypes.contains(Factory.OntologyType.fromQName("DBpedia:City")))
//        assert(dBpediaResource.getTypes.contains(Factory.OntologyType.fromQName("Freebase:/location/location")))
//    }
//
//
//    @Test
//    def specificDBpediaResourceWithEmptyTypes() {
//        val dBpediaResource: DBpediaResource = Factory.DBpediaResource.from("Giant_oil_and_gas_fields")
//        assert(dBpediaResource.getTypes.size() == 0)
//    }
//
//
//    @Test
//    def uriToSurfaceForm() {
//        val examples = Map("Huge"->"Huge",
//            "Huge_(TV_series)"->"Huge",
//            "Huge_cardinal"->"Huge cardinal",
//            "Apple_(disambiguation)"->"Apple",
//            "Apple_%28disambiguation%29"->"Apple");
//
//        examples.keys.foreach( title => {
//            val r = new DBpediaResource(title)
//            val s = Factory.createSurfaceFormFromDBpediaResourceURI(r, false)
//            printf("%-30s=%30s \n",title,r.uri)
//            printf("%-30s=%30s \n",examples(title),s.name)
//            assert(s.name.equals(examples(title)))
//        })
//    }
//
//
//}