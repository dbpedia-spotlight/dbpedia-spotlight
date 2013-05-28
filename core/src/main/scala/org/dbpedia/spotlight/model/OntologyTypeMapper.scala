package org.dbpedia.spotlight.msm2013.model

import org.dbpedia.spotlight.model.{OntologyType, DBpediaResource}

/**
 * Interface for providing ontology types for URIs. Implementations can be in-mem, in disk, and using different backends.
 * Given a URI, returns a set of types that this URI is known to have in the ontology.
 * @author dirk, pablomendes
 */
trait OntologyTypeMapper {

    def getTypesForResource(resource: DBpediaResource): Set[OntologyType]

}
