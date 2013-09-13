package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.log.SpotlightLog
import java.io.File
import org.dbpedia.spotlight.util.IndexingConfiguration
import org.dbpedia.spotlight.model.Factory
import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.store.FSDirectory

/**
 * This class writes splitted indexes for splitted occurrences (this can be done topically, see package topic).
 *
 * @author dirk
 */
object IndexSplittedOccurences {
  /**
   *
   * Usage: mvn scala:run -DmainClass=org.dbpedia.spotlight.lucene.index.IndexSplittedOccurences "-DaddArgs=$INDEX_CONFIG_FILE|output/to/splitted/occs[|overwrite]"
   */
  def main(args:Array[String]) {
    val parentFile = new File(args(1))
    
    val indexingConfigFileName = args(0)

    var shouldOverwrite = false
    if (args.length>2) {
      if (args(2).toLowerCase.contains("overwrite"))
        shouldOverwrite = true
    }

    val config = new IndexingConfiguration(indexingConfigFileName)

    // Command line options
    val baseDir = config.get("org.dbpedia.spotlight.index.dir")   //getBaseDir(args(1))
    val similarity = Factory.Similarity.fromName("InvCandFreqSimilarity")  //config.getSimilarity(args(2))
    val analyzer = config.getAnalyzer  //config.getAnalyzer(args(3))

    SpotlightLog.info(this.getClass, "Output index to: %s", baseDir)
    SpotlightLog.info(this.getClass, "Similarity class: %s", similarity.getClass)
    SpotlightLog.info(this.getClass, "Analyzer class: %s", analyzer.getClass)

    SpotlightLog.warn(this.getClass, "WARNING: this process will run a lot faster if the occurrences are sorted by URI!")

    val minNumDocsBeforeFlush : Int = config.get("org.dbpedia.spotlight.index.minDocsBeforeFlush", "200000").toInt
    val lastOptimize = false

    parentFile.listFiles().foreach( occsFile => {
      //val indexOutputDir = baseDir+"2.9.3/Index.wikipediaTraining.Merged."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName
      val indexOutputDir = baseDir+"/"+occsFile.getName.substring(0, occsFile.getName.indexOf("."))

      val lucene = new LuceneManager.BufferedMerging(FSDirectory.open(new File(indexOutputDir)),
        minNumDocsBeforeFlush,
        lastOptimize)
      lucene.setContextSimilarity(similarity)
      lucene.setDefaultAnalyzer(analyzer)
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
      SpotlightLog.info(this.getClass, "Max memory: %fGB", +Runtime.getRuntime.maxMemory / 1073741824.0)
      /* Total memory currently in use by the JVM */
      SpotlightLog.info(this.getClass, "Total memory (bytes): %fGB", Runtime.getRuntime.totalMemory / 1073741824.0)
      //SpotlightLog.info(this.getClass, "MinNumDocsBeforeFlush: %d", minNumDocsBeforeFlush)

      IndexMergedOccurrences.index(occsFile.getAbsolutePath, vectorBuilder)
    } )

    SpotlightLog.info(this.getClass, "Indexes saved to: %s", baseDir)
  }
}
