/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.lucene.index

import java.io.File
import org.dbpedia.spotlight.io.{FileOccurrenceSource}
import org.dbpedia.spotlight.log.SpotlightLog
import org.apache.lucene.store.FSDirectory
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.util.IndexingConfiguration
import org.dbpedia.spotlight.model.Factory

/**
 * Indexes all occurrences of a DBpedia Resource in Wikipedia as a Lucene index where each document represents one resource.
 *
 * @author maxjakob
 */
object IndexMergedOccurrences
{
    def index(trainingInputFile : String, indexer: OccurrenceContextIndexer ) {
        val wpOccurrences = FileOccurrenceSource.fromFile(new File(trainingInputFile))
        var indexDisplay = 0
        SpotlightLog.info(this.getClass, "Indexing with %s in Lucene ...", indexer.getClass)

        wpOccurrences.foreach( occurrence => {
            try {

            indexer.add(occurrence)

            indexDisplay += 1
            if (indexDisplay % 10000 == 0) {
                SpotlightLog.debug(this.getClass, "  indexed %d occurrences", indexDisplay)
            }
            } catch {
                case e: Exception => {
                    SpotlightLog.error(this.getClass, "Error parsing %d. ", indexDisplay)
                    e.printStackTrace()
                }
            }
        })
        indexer.close  // important

        SpotlightLog.info(this.getClass, "Finished: indexed %d occurrences", indexDisplay)
    }

    def getBaseDir(baseDirName : String) : String = {
        if (! new File(baseDirName).exists) {
            println("Base directory not found! "+baseDirName);
            exit();
        }
        baseDirName
    }

    /**
     *
     * Usage: mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.IndexMergedOccurrences "-DaddArgs=$INDEX_CONFIG_FILE|output/occs.uriSorted.tsv|overwrite"
     */
    def main(args : Array[String])
    {
        val indexingConfigFileName = args(0)
        val trainingInputFileName = args(1)

        var shouldOverwrite = false
        if (args.length>2) {
            if (args(2).toLowerCase.contains("overwrite"))
                shouldOverwrite = true;
        }

        val config = new IndexingConfiguration(indexingConfigFileName)

        // Command line options
        val baseDir = config.get("org.dbpedia.spotlight.index.dir")   //getBaseDir(args(1))
        val similarity = Factory.Similarity.fromName("InvCandFreqSimilarity")  //config.getSimilarity(args(2))
        val analyzer = config.getAnalyzer  //config.getAnalyzer(args(3))

        SpotlightLog.info(this.getClass, "Using dataset under: %s", baseDir)
        SpotlightLog.info(this.getClass, "Similarity class: %s", similarity.getClass)
        SpotlightLog.info(this.getClass, "Analyzer class: %s", analyzer.getClass)

        SpotlightLog.warn(this.getClass, "WARNING: this process will run a lot faster if the occurrences are sorted by URI!")

        val minNumDocsBeforeFlush : Int = config.get("org.dbpedia.spotlight.index.minDocsBeforeFlush", "200000").toInt
        val lastOptimize = false;

        //val indexOutputDir = baseDir+"2.9.3/Index.wikipediaTraining.Merged."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName;
        val indexOutputDir = baseDir

        val lucene = new LuceneManager.BufferedMerging(FSDirectory.open(new File(indexOutputDir)),
                                                        minNumDocsBeforeFlush,
                                                        lastOptimize)
        lucene.setContextSimilarity(similarity);
        lucene.setDefaultAnalyzer(analyzer);
        // If the index directory does not exist, tell lucene to overwrite.
        // If it exists, the user has to indicate in command line that he/she wants to overwrite it.
        // I chose command line instead of configuration file to force the user to look at it before running the command.
        if (!new File(indexOutputDir).exists()) {
            lucene.shouldOverwrite = true
            new File(indexOutputDir).mkdir()
        } else {
            lucene.shouldOverwrite = shouldOverwrite
        }

        val vectorBuilder = new MergedOccurrencesContextIndexer(lucene)

        val freeMemGB : Double = Runtime.getRuntime.freeMemory / 1073741824.0
        if (Runtime.getRuntime.freeMemory < minNumDocsBeforeFlush) SpotlightLog.error(this.getClass, "Your available memory %fGB is less than minNumDocsBeforeFlush. This setting is known to give OutOfMemoryError.", freeMemGB)
        SpotlightLog.info(this.getClass, "Available memory: %fGB", freeMemGB)
        SpotlightLog.info(this.getClass, "Max memory: %fGB", Runtime.getRuntime.maxMemory / 1073741824.0)
        /* Total memory currently in use by the JVM */
        SpotlightLog.info(this.getClass, "Total memory (bytes): %fGB", Runtime.getRuntime.totalMemory / 1073741824.0)
        //SpotlightLog.info("MinNumDocsBeforeFlush: "+minNumDocsBeforeFlush, this.getClass)
        
        index(trainingInputFileName, vectorBuilder);

        SpotlightLog.info(this.getClass, "Index saved to: %s", indexOutputDir)
        
    }

}