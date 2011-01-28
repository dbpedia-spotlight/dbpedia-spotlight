package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

/**
 * Represents a source of context from disambiguation page links
 */

trait DisambiguationSource extends Traversable[DBpediaResourceOccurrence]