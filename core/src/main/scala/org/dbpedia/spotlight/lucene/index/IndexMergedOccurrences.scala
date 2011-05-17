/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.lucene.index

import java.io.File
import org.dbpedia.spotlight.io.{FileOccurrenceSource, LuceneIndexWriter}
import org.apache.commons.logging.LogFactory
import org.apache.lucene.store.FSDirectory
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.util.IndexingConfiguration

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
        val indexingConfigFileName = args(0)
        val trainingInputFileName = args(1)

        val config = new IndexingConfiguration(indexingConfigFileName)

        // Command line options
        val baseDir = config.get("org.dbpedia.spotlight.index.dir")   //getBaseDir(args(1))
        val similarity = config.getSimilarity("InvCandFreqSimilarity")  //config.getSimilarity(args(2))
        val analyzer = config.getAnalyzer("StandardAnalyzer")  //config.getAnalyzer(args(3))

        LOG.info("Using dataset under: "+baseDir);
        LOG.info("Similarity class: "+similarity.getClass);
        LOG.info("Analyzer class: "+analyzer.getClass);

        LOG.warn("WARNING: this process will run a lot faster if the occurrences are sorted by URI!");

        val minNumDocsBeforeFlush : Int = 200000 //IndexConfiguration.properties.getProperty("minNumDocsBeforeFlush").toDouble
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
        
        index(trainingInputFileName, vectorBuilder);

        config.set("org.dbpedia.spotlight.index.dir", indexOutputDir)

        LOG.info("Index saved to: "+indexOutputDir );
        
    }

}