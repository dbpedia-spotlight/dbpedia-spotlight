package org.dbpedia.spotlight.extract

import org.dbpedia.spotlight.model._

/**
 * A TagExtractor is almost like a Disambiguator, but it does not constrain the tags to things that were *mentioned* in text.
 * A concept/entity that was not mentioned but is related should also be returned.
 *
 * @author pablomendes
 */
trait TagExtractor {

    /**
     * Extract a ranked list of DBpedia Resources
     */
    def extract(text: Text, nTags: Int) : Seq[(DBpediaResource,Double)]

    def extract(text: Text, nTags: Int, ontologyTypes: List[OntologyType]) : Seq[(DBpediaResource,Double)]

}
