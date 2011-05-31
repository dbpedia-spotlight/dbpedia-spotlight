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

package org.dbpedia.spotlight.evaluation

import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.io.{LuceneIndexWriter, FileOccurrenceSource}
import org.apache.commons.logging.LogFactory
import java.io.{PrintStream, File}
import org.apache.lucene.misc.SweetSpotSimilarity
import org.apache.lucene.search.{Similarity, DefaultSimilarity}
import org.dbpedia.spotlight.lucene.disambiguate._
import org.dbpedia.spotlight.lucene.similarity._

import  org.dbpedia.spotlight.util.Profiling._
import org.apache.lucene.store.{NIOFSDirectory, Directory, FSDirectory}
import io.Source
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.disambiguate._
import mixtures.LinearRegressionMixture
import org.dbpedia.spotlight.lucene.index.MergedOccurrencesContextIndexer
import org.dbpedia.spotlight.lucene._
import search.{CandidateSearcher, MergedOccurrencesContextSearcher}
import org.dbpedia.spotlight.model.{SpotlightConfiguration, ContextSearcher}

/**
 * This class is evolving to be the main disambiguation class that takes parameters for which dataset to run.
 *
 * @author maxjakob
 */
object EvaluateDisambiguationOnly
{

    private val LOG = LogFactory.getLog(this.getClass)

    def createMergedDisambiguator(outputFileName: String, analyzer: Analyzer, similarity: Similarity) : Disambiguator = {
        val directory = FSDirectory.open(new File(outputFileName));//+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
        createMergedDisambiguator(outputFileName, analyzer, similarity, directory)
    }

    def createMergedDisambiguator(outputFileName: String, analyzer: Analyzer, similarity: Similarity, directory: Directory) : Disambiguator = {
        createDisambiguator(outputFileName, analyzer, similarity, directory, new MergedOccurrencesDisambiguator(_))
    }

    def createDisambiguator(outputFileName: String, analyzer: Analyzer, similarity: Similarity, directory: Directory, dis: (MergedOccurrencesContextSearcher => Disambiguator)) : Disambiguator = {
        ensureExists(directory)
        //val luceneManager = new LuceneManager.BufferedMerging(directory)
        val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
        //val isfLuceneManager = new LuceneManager.BufferedMerging(new RAMDirectory())

//        val queryTimeAnalyzer = new QueryAutoStopWordAnalyzer(Version.LUCENE_29, analyzer);
        val queryTimeAnalyzer = analyzer;

        luceneManager.setContextAnalyzer(queryTimeAnalyzer);
        luceneManager.setContextSimilarity(similarity);
        //------------ ICF DISAMBIGUATOR
        val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);

        //timed(printTime("Adding auto stopwords took ")) {
        //  queryTimeAnalyzer.addStopWords(contextSearcher.getIndexReader, DBpediaResourceField.CONTEXT.toString, 0.5f);
        //}

        //timed(printTime("Warm up took ")) {
        //  contextSearcher.warmUp(10000);
        //}

        LOG.info("Number of entries in merged resource index ("+contextSearcher.getClass()+"): "+ contextSearcher.getNumberOfEntries());
        // The Disambiguator chooses the best URI for a surface form
        dis(contextSearcher)
    }

    def getNewStopwordedDisambiguator(indexDir: String) : Disambiguator = {
        val f = new File("e:\\dbpa\\data\\surface_forms\\stopwords.list")
        //val stopwords = Source.fromFile(f, "UTF-8").getLines.toSet
        val stopwords = StopAnalyzer.ENGLISH_STOP_WORDS_SET
        println("Stopwords loaded: "+stopwords.size);
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", stopwords);
        val similarity : Similarity = new InvCandFreqSimilarity();
        //val directory =  LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
        val directory =  LuceneManager.pickDirectory(new File(indexDir));
        createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }

    def getICFSnowballDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new InvCandFreqSimilarity();
        //val directory =  LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
        val directory =  LuceneManager.pickDirectory(new File(indexDir));
        createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }

    def getICFCachedDisambiguator(indexDir: String) : Disambiguator = {
      val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      //val directory = LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
      val directory =  LuceneManager.pickDirectory(new File(indexDir));
      val cache = JCSTermCache.getInstance(new LuceneManager.BufferedMerging(directory), 5000);
      val similarity : Similarity = new CachedInvCandFreqSimilarity(cache);
      createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }

    def getICFCachedMixedDisambiguator(indexDir: String) : Disambiguator = {
      val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      //val directory = LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
      val directory =  LuceneManager.pickDirectory(new File(indexDir));
      val cache = JCSTermCache.getInstance(new LuceneManager.BufferedMerging(directory),5000);
      //val similarity : Similarity = new CachedInvCandFreqSimilarity(cache);
      val similarity : Similarity = new InvCandFreqSimilarity
      val mixture = new LinearRegressionMixture
      createDisambiguator(indexDir, analyzer, similarity, directory, new MixedWeightsDisambiguator(_, mixture))
    }

    def getNewDisambiguator(indexDir: String) : Disambiguator = {
      val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      val directory = LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
      val cache = JCSTermCache.getInstance(new LuceneManager.BufferedMerging(directory),5000);
      val similarity : Similarity = new NewSimilarity(cache);
      createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }


    def getICFStandardDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_29, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new InvCandFreqSimilarity();
        val directory = FSDirectory.open(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
        createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }

    def getDefaultSnowballDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity();
        createMergedDisambiguator(indexDir, analyzer, similarity)
    }

    def getDefaultStandardDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_29, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity();
        createMergedDisambiguator(indexDir, analyzer, similarity)
    }

    def getSweetSpotSnowballDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new SweetSpotSimilarity()
        createMergedDisambiguator(indexDir, analyzer, similarity)
    }

    def getSweetSpotStandardDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_29, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new SweetSpotSimilarity()
        createMergedDisambiguator(indexDir, analyzer, similarity)
    }


    // the next two use an own disambiguator, while the two before just use a different similarity class

//    def getDefaultScorePlusPriorSnowballDisambiguator(indexDir: String) : Disambiguator = {
//        var analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
//        var similarity : Similarity = new DefaultSimilarity();
//        val directory = FSDirectory.open(new File(indexDir+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
//
//        ensureExists(directory)
//        val luceneManager = new LuceneManager.BufferedMerging(directory)
//        //val isfLuceneManager = new LuceneManager.BufferedMerging(new RAMDirectory())
//        luceneManager.setContextAnalyzer(analyzer);
//        luceneManager.setContextSimilarity(similarity);
//        //------------ ICF DISAMBIGUATOR
//        val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);
//        LOG.info("Number of entries in merged resource index ("+contextSearcher.getClass()+"): "+ contextSearcher.getNumberOfEntries());
//        // The Disambiguator chooses the best URI for a surface form
//        new MergedPlusPriorDisambiguator(contextSearcher)
//    }







//    def getICFIDFSnowballDisambiguator(indexDir: String) : Disambiguator = {
//        var analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
//        var similarity : Similarity = new ICFIDFSimilarity();
//        createMergedDisambiguator(indexDir, analyzer, similarity, FSDirectory.open(new File(indexDir+"."+analyzer.getClass.getSimpleName+".InvSenseFreqSimilarity")))
//    }
//
//    def getICFIDFStandardDisambiguator(indexDir: String) : Disambiguator = {
//        var analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_29, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
//        var similarity : Similarity = new ICFIDFSimilarity();
//        createMergedDisambiguator(indexDir, analyzer, similarity, FSDirectory.open(new File(indexDir+"."+analyzer.getClass.getSimpleName+".InvSenseFreqSimilarity")))
//    }

    def getPriorDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity
        val directory = new NIOFSDirectory(new File(indexDir));//+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
        //val luceneManager = new LuceneManager.BufferedMerging(directory)
        val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
        luceneManager.setContextAnalyzer(analyzer);
        luceneManager.setContextSimilarity(similarity);
        val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);
        LOG.info("Number of entries in merged resource index ("+contextSearcher.getClass()+"): "+ contextSearcher.getNumberOfEntries());
        new LucenePriorDisambiguator(contextSearcher)
    }

    def getRandomDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity
        val directory = new NIOFSDirectory(new File(indexDir));//+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
        //val luceneManager = new LuceneManager.BufferedMerging(directory)
        val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
        luceneManager.setContextAnalyzer(analyzer);
        luceneManager.setContextSimilarity(similarity);
        val contextSearcher = new CandidateSearcher(luceneManager);
        LOG.info("Number of entries in merged resource index ("+contextSearcher.getClass()+"): "+ contextSearcher.getNumberOfEntries());
        new RandomDisambiguator(contextSearcher)
    }

    def ensureExists(directory: Directory)() {
        // Lucene Indexer - Needs an index in disk to be used in disambiguation
//        if (!directory.getFile.exists)
//        {
////                LOG.info("Index directory does not exist. Will index.");
////                index(trainingFile, isfLuceneManager);
//            System.err.println("Index directory does not exist. "+directory.getFile);
//            exit();
//        } else {
//            LOG.info("Index directory exists ("+directory.getFile+"). Will not reindex.");
//        }
    }

    def index(trainingFile: File, luceneManager: LuceneManager.BufferedMerging) {
            val trainSource = FileOccurrenceSource.fromFile(trainingFile)
            val vectorBuilder = new MergedOccurrencesContextIndexer(luceneManager);
            LuceneIndexWriter.writeLuceneIndex(vectorBuilder, trainSource)
            LOG.info("Number of entries indexed: "+vectorBuilder.numEntriesProcessed);
    }

    def exists(fileName: String) {
        if (!new File(fileName).exists) {
            System.err.println("Important directory does not exist. "+fileName);
            exit(1);
        }
    }

    def main(args : Array[String])
    {

        //val indexDir: String = args(0)  //"e:\\dbpa\\data\\index\\index-that-works\\Index.wikipediaTraining.Merged."
        val config = new SpotlightConfiguration(args(0));
        val indexDir = config.getIndexDirectory

        val simScoresFileName: String = ""

        val testFileName: String = args(1)  //"e:\\dbpa\\data\\index\\dbpedia36data\\test\\test100k.tsv"
        val resultsFileName: String = testFileName+".log"


        val out = new PrintStream(resultsFileName, "UTF-8");

        //exists(indexDir);
        exists(testFileName);
        //exists(resultsFileName);

        val osName : String = System.getProperty("os.name");
        LOG.info("Your operating system is: "+osName);

        //            val trainingFile = new File(baseDir+"wikipediaTraining.50.amb.tsv")
        //            if (!trainingFile.exists) {
        //                System.err.println("Training file does not exist. "+trainingFile);
        //                exit();
        //            }


        //            val trainingFileName = baseDir+"wikipediaAppleTraining.50.amb.tsv"
        //            val luceneIndexFileName = baseDir+"2.9.3/MergedIndex.wikipediaAppleTraining.a50"
        //            val testFileName = baseDir+"wikipediaAppleTest.50.amb.tsv"

        // For merged disambiguators
        //val indexDir = baseDir+"/2.9.3/Index.wikipediaTraining.Merged";

        val default : Disambiguator = new DefaultDisambiguator(config)
        val disSet = Set(
                            default,
                            getDefaultSnowballDisambiguator(indexDir) ,
                            getICFCachedDisambiguator(indexDir),
                            //getICFCachedMixedDisambiguator(indexDir),
                            //getNewStopwordedDisambiguator(indexDir),
                            //getICFSnowballDisambiguator(indexDir)
                            //getSweetSpotSnowballDisambiguator(indexDir)
                            //getICFWithPriorSnowballDisambiguator(indexDir),
                            //getICFIDFSnowballDisambiguator(indexDir),
                            //getNewDisambiguator(indexDir),

                            //getDefaultScorePlusPriorSnowballDisambiguator(indexDir)
//                                getDefaultScorePlusConditionalSnowballDisambiguator(indexDir),
//                                getProbPlusPriorSnowballDisambiguator(indexDir),
//                                getProbPlusConditionalSnowballDisambiguator(indexDir)

                         // Standard analyzer
                            //getDefaultStandardDisambiguator(indexDir),
                            //getICFStandardDisambiguator(indexDir),
                            //getSweetSpotStandardDisambiguator(indexDir),
                            //getICFWithPriorStandardDisambiguator(indexDir),
                            //getICFIDFStandardDisambiguator(indexDir),
                            //getICFIDFStandardDisambiguator,

                         // no analyzer
                            getPriorDisambiguator(indexDir),
                            getRandomDisambiguator(indexDir)
        )

        // Read some text to test.
        val testSource = FileOccurrenceSource.fromFile(new File(testFileName))

        //      // Now with most common amongst top K most similar contexts
        //      var disambiguator2 : Disambiguator = new MergedOccurrencesDisambiguator(resourceSearcher, new RankingStrategy.MostCommonAmongTopK(contextSearcher))
        //      // Now with most common URI by usage (prior) -- reading from file
        //      //var disambiguator3 : Disambiguator = new CustomScoresDisambiguator(surrogateSearcher, new DataLoader(new DataLoader.TSVParser(), new File(resourcePriorsFileName)));
        //      // Now with most common URI by usage (prior) -- reading from lucene
        //      var disambiguator4 : Disambiguator = new LucenePriorDisambiguator(resourceSearcher);


        //        LOG.info("=================================");

          //testSource.view(10000,15000)
          val evaluator = new DisambiguationEvaluator(testSource, disSet, out);
          evaluator.evaluate()
          out.close();

    }

}