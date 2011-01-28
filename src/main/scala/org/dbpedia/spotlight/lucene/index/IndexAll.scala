package org.dbpedia.spotlight.lucene.index

import java.io.File
import org.apache.lucene.store.FSDirectory
import org.dbpedia.spotlight.io.{LuceneIndexWriter, FileOccurrenceSource}
import org.dbpedia.spotlight.lucene.LuceneManager

/**
 * Indexes all Occurrences in Wikipedia separately in a Lucene index.
 */

object IndexAll
{

    def main(args : Array[String])
    {
        // Will read occurrences from this file
        val inputFileName = "wikipediaTraining.50.ambiguous.tsv"
        val wpOccurrences = FileOccurrenceSource.fromFile(new File("data/"+inputFileName))
        // Set up a lucene manager for separate occurrences
        val luceneSepOcc = new LuceneManager(FSDirectory.open(new File("data/SepOccIndex."+inputFileName)))
        // Also set up a lucene manager for merged occurrences
        val minNumDocsBeforeFlush = 10000 //ConfigProperties.properties.getProperty("minNumDocsBeforeFlush").toDouble
        val luceneMerged = new LuceneManager.BufferedMerging(FSDirectory.open(new File("data/Merged."+inputFileName)), minNumDocsBeforeFlush)

        // Create the indexer for separate occurrences
        val sepOccVectorBuilder = new SeparateOccurrencesIndexer(luceneSepOcc)
        LuceneIndexWriter.writeLuceneIndex(sepOccVectorBuilder, wpOccurrences)

        // Create the indexer for merged occurrences
        val mergedVectorBuilder = new MergedOccurrencesContextIndexer(luceneMerged)
        LuceneIndexWriter.writeLuceneIndex(mergedVectorBuilder, wpOccurrences)

    }

}