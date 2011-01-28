package org.dbpedia.spotlight.io

import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.lucene.index.{BaseIndexer, OccurrenceContextIndexer, MergedOccurrencesContextIndexer}

/**
 * Writes Lucene index.
 */


object LuceneIndexWriter
{
    private val LOG = LogFactory.getLog(this.getClass)

    def writeLuceneIndex(indexer : OccurrenceContextIndexer, occSource : OccurrenceSource)
    {
        var indexDisplay = 0
        LOG.info("Indexing with " + indexer.getClass + " in Lucene ...")

        for (occ <- occSource.view)
        {
            indexer.add(occ)

            indexDisplay += 1
            if (indexDisplay % 10000 == 0) {
                LOG.debug("  indexed " + indexDisplay + " occurrences")
            }
        }
        indexer.close  // important

        LOG.info("Finished: indexed " + indexDisplay + " occurrences")
    }

}