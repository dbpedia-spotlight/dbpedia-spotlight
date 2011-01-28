package org.dbpedia.spotlight.io

import java.io.File
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{DBpediaType, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.util.TypesLoader

/**
 * User: Max
 * Date: 27.08.2010
 * Time: 14:44:52
 * Adds types to Occurrences
 */

class TypeAdder(val occSource : OccurrenceSource, var typesMap : Map[String,List[DBpediaType]]) extends OccurrenceSource
{
    private val LOG = LogFactory.getLog(this.getClass)
    
    def this(occSource : OccurrenceSource, typesFile : File) = {
        this(occSource, TypesLoader.getTypesMap(typesFile))
    }

    override def foreach[U](f : DBpediaResourceOccurrence => U) {
        for (occ <- occSource) {
            if (occ.resource.types.isEmpty) {
                occ.resource.types = typesMap.get(occ.resource.uri).getOrElse(List[DBpediaType]())
                f( new DBpediaResourceOccurrence(occ.id,
                                                 occ.resource,
                                                 occ.surfaceForm,
                                                 occ.context,
                                                 occ.textOffset,
                                                 occ.provenance,
                                                 occ.similarityScore) )
            }
            else {
                f( occ )
            }
        }
    }
   
}