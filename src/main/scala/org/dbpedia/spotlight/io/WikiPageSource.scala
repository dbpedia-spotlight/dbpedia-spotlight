package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.WikiPageContext

/**
 * Represents a source of WikiPageContext objects.
 */

trait WikiPageSource extends Traversable[WikiPageContext]