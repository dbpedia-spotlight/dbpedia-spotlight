/*
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.model

import org.junit.Test
import org.dbpedia.spotlight.exceptions.ItemNotFoundException

/**
 * For tests that use a store (Lucene or HSQL) to create objects.
 *
 * @author Joachim Daiber
 */

class StoreBackedFactoryTests {

    val configuration: SpotlightConfiguration = new SpotlightConfiguration("conf/server.properties")
    val spotlightFactory: SpotlightFactory = new SpotlightFactory(configuration)

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
        //assert(dBpediaResource.getTypes.contains(Factory.OntologyType.fromQName("Freebase:/location/location")))
        assert(dBpediaResource.getTypes.contains(Factory.OntologyType.fromQName("DBpedia:City")))
        assert(dBpediaResource.getTypes.contains(Factory.OntologyType.fromQName("Schema:City")))

    }

    @Test
    def specificDBpediaResourceWithEmptyTypes() {
        val dBpediaResource: DBpediaResource = spotlightFactory.DBpediaResource.from("Giant_oil_and_gas_fields")
        assert(dBpediaResource.getTypes.size() == 0)
    }

}