package org.dbpedia.spotlight.io

import org.apache.lucene.document.Document
import org.dbpedia.spotlight.lucene.LuceneManager
import java.io.File
import org.dbpedia.spotlight.model._
import org.junit.Test
import org.apache.lucene.store.Directory
import org.apache.lucene.search.Similarity
import org.dbpedia.spotlight.lucene.similarity.{JCSTermCache, CachedInvCandFreqSimilarity}
import org.dbpedia.spotlight.lucene.search.{BaseSearcher, MergedOccurrencesContextSearcher}

/**
 * Allows you to iterate over DBpedia Spotlight's index.
 * i.e. Traverses the index returning each URI stored through an Iterable
 *
 * TODO each occurrence here is actually a merge of several occurrences that were previously indexed. should be one occurrence each
 * TODO should have two classes: MergedOccurrencesSource and IndexedOccurrencesSource
 * TODO could also have a source of surface forms and URIs from the index.
 *
 * @author pablomendes
 */

object IndexedOccurrencesSource {

    def fromConfigFile(configFile : File) : OccurrenceSource = {
        val factory = new SpotlightFactory(new SpotlightConfiguration(configFile.getAbsolutePath))
        new IndexedOccurrencesSource(factory.contextSearcher)
    }

    def fromFile(file : File) : OccurrenceSource = {
        val directory : Directory = LuceneManager.pickDirectory(file)
        val luceneManager : LuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
        val searcher = new MergedOccurrencesContextSearcher(luceneManager);
        new IndexedOccurrencesSource(searcher)
    }

    private class IndexedOccurrencesSource(searcher: MergedOccurrencesContextSearcher) extends BaseIndexSource(searcher, Factory.createDBpediaResourceOccurrencesFromDocument);

    private class MergedOccurrencesSource(searcher: MergedOccurrencesContextSearcher) extends BaseIndexSource(searcher, Factory.createMergedDBpediaResourceOccurrenceFromDocument);

    /**
     * Returns an occurrence source with occurrences extracted from the index.
     * TODO currently only works for the indexes that were not compacted (because they have the field CONTEXT)
     */
    private class BaseIndexSource (val searcher: MergedOccurrencesContextSearcher, val create : (Document, Int, BaseSearcher) => Array[DBpediaResourceOccurrence] = Factory.createDBpediaResourceOccurrencesFromDocument) extends OccurrenceSource {

        val indexSize: Int = searcher.getNumberOfEntries
        val fivePercentOfIndex = 0.5 * indexSize

        override def foreach[U](f : DBpediaResourceOccurrence => U) : Unit = {

            val range = 0 until indexSize
            for(i <- range) {
                if (! searcher.isDeleted(i)) {
                    // create occurrence
                    val doc: Document = searcher.getFullDocument(i)

                    val occurrences = create(doc, i, searcher);

                    // apply closure on the occurrences
                    occurrences.foreach(o => f(o));

                    // report
                    //if (i % fivePercentOfIndex == 0) {
                    //LOG.trace("  processed " + i + " URIs.")
                    //}
                }
            }
        }

    }

}
