package org.dbpedia.spotlight.lucene.index

import java.io.File
import org.dbpedia.spotlight.io.{FileOccurrenceSource, LuceneIndexWriter, WikiOccurrenceSource}
import org.apache.commons.logging.LogFactory
import org.apache.lucene.store.FSDirectory
import org.dbpedia.spotlight.lucene.{LuceneManager}
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.{StopAnalyzer, StopFilter, Analyzer}
import org.apache.lucene.misc.SweetSpotSimilarity
import org.apache.lucene.search.{DefaultSimilarity, Similarity}
import org.dbpedia.spotlight.lucene.similarity.InvCandFreqSimilarity
import org.apache.lucene.analysis.snowball.SnowballAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer

/**
 * Indexes all Occurrences in Wikipedia separately in a Lucene index.
 */

object IndexMergedOccurrences
{
    private val LOG = LogFactory.getLog(this.getClass)

    def index(trainingInputFile : String, vectorBuilder: OccurrenceContextIndexer ) {
        //val wpDumpFileName = ConfigProperties.properties.getProperty("WikipediaXMLDump")
        val wpOccurrences = FileOccurrenceSource.fromFile(new File(trainingInputFile))

        //wpOccurrences.view(0,100).foreach(println(_))
        LuceneIndexWriter.writeLuceneIndex(vectorBuilder, wpOccurrences)
    }
    //TODO Move to factory class (Maybe ConfigProperties?)
    def getSimilarity(similarityName : String) : Similarity = {
        val validSimilarities = List[Similarity](new InvCandFreqSimilarity, new SweetSpotSimilarity, new DefaultSimilarity);
        
        validSimilarities.find(s => s.getClass.getSimpleName equals similarityName) match {
            case Some(similarity) => { similarity }
            case None => {
                println("Unknown Similarity: "+similarityName)
                exit();
            }
        }
    }
    //TODO Move to factory class (Maybe ConfigProperties?)
    def getAnalyzer(analyzerName : String) : Analyzer = {
        val validAnalyzers = List[Analyzer](new StandardAnalyzer(Version.LUCENE_29), new SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET))

        validAnalyzers.find(a => a.getClass.getSimpleName equals analyzerName) match {
            case Some(analyzer) => { analyzer }
            case None => {
                println("Unknown Analyzer: "+analyzerName)
                exit();
            }
        }
    }

    def getBaseDir(baseDirName : String) : String = {
        //var baseDir: String = "C:\\Users\\Pablo\\workspace\\dbpa\\data\\Company\\"
        //var baseDir: String = "C:\\Users\\Pablo\\workspace\\dbpa\\data\\PopulatedPlace\\"
        //var baseDir: String = "e:/dbpa/data/Person/"
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
        val similarity = getSimilarity(args(1))
        val analyzer = getAnalyzer(args(2))

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

        var freeMemGB : Double = Runtime.getRuntime.freeMemory / 1073741824
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