package org.dbpedia.spotlight.lucene.index

import java.io.File
import org.dbpedia.spotlight.io.{FileOccurrenceSource, LuceneIndexWriter}
import org.apache.commons.logging.LogFactory
import org.apache.lucene.store.FSDirectory
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.util.ConfigProperties

/**
 * Indexes all Occurrences in Wikipedia separately in a Lucene index.
 */

object IndexMergedOccurrences
{
    private val LOG = LogFactory.getLog(this.getClass)

    def index(trainingInputFile : String, vectorBuilder: OccurrenceContextIndexer ) {
        val wpOccurrences = FileOccurrenceSource.fromFile(new File(trainingInputFile))
        LuceneIndexWriter.writeLuceneIndex(vectorBuilder, wpOccurrences)
    }

    def getBaseDir(baseDirName : String) : String = {
        if (! new File(baseDirName).exists) {
            println("Base directory not found! "+baseDirName);
            exit();
        }
        baseDirName
    }

    def main(args : Array[String])
    {
        // Command line options
        val baseDir = getBaseDir(args(0))
        val similarity = ConfigProperties.getSimilarity(args(1))
        val analyzer = ConfigProperties.getAnalyzer(args(2))

        LOG.info("Using dataset under: "+baseDir);
        LOG.info("Similarity class: "+similarity.getClass);
        LOG.info("Analyzer class: "+analyzer.getClass);

        LOG.warn("WARNING: this process will run a lot faster if the occurrences are sorted by URI!");

        val trainingInputFile = baseDir+"training.tsv";
        val minNumDocsBeforeFlush : Int = 200000 //ConfigProperties.properties.getProperty("minNumDocsBeforeFlush").toDouble
        val lastOptimize = false;

        val indexOutputDir = baseDir+"2.9.3/Index.wikipediaTraining.Merged."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName;

        val lucene = new LuceneManager.BufferedMerging(FSDirectory.open(new File(indexOutputDir)),
                                                        minNumDocsBeforeFlush,
                                                        lastOptimize)
        lucene.setContextSimilarity(similarity);
        lucene.setContextAnalyzer(analyzer);

        val vectorBuilder = new MergedOccurrencesContextIndexer(lucene)

        val freeMemGB : Double = Runtime.getRuntime.freeMemory / 1073741824
        if (Runtime.getRuntime.freeMemory < minNumDocsBeforeFlush) LOG.error("Your available memory "+freeMemGB+"GB is less than minNumDocsBeforeFlush. This setting is known to give OutOfMemoryError.");
        LOG.info("Available memory: "+freeMemGB+"GB")
        LOG.info("Max memory: "+Runtime.getRuntime.maxMemory / 1073741824.0 +"GB")
        /* Total memory currently in use by the JVM */
        LOG.info("Total memory (bytes): " + Runtime.getRuntime.totalMemory / 1073741824 + "GB")
        //LOG.info("MinNumDocsBeforeFlush: "+minNumDocsBeforeFlush)
        
        index(trainingInputFile, vectorBuilder);

        LOG.info("Index saved to: "+indexOutputDir );
        
    }

}