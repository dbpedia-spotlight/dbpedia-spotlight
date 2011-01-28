package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

/**
 * Represents a source of Occurrences
 */

trait OccurrenceSource extends Traversable[DBpediaResourceOccurrence]