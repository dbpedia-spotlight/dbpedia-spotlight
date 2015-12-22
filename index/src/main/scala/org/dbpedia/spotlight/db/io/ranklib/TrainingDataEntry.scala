package org.dbpedia.spotlight.db.io.ranklib

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

/**
 * Created by dowling on 02/08/15.
 */
class TrainingDataEntry(val correctOccurrence: DBpediaResourceOccurrence, val predictedOccurrences: List[DBpediaResourceOccurrence] ) {

}
