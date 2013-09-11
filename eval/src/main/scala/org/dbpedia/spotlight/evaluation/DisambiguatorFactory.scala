package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.disambiguate.mixtures.LinearRegressionMixture
import org.dbpedia.spotlight.lucene.similarity._
import org.apache.lucene.misc.SweetSpotSimilarity
import org.dbpedia.spotlight.lucene.disambiguate.{LucenePriorDisambiguator, MixedWeightsDisambiguator, MergedOccurrencesDisambiguator}
import org.apache.lucene.search.{DefaultSimilarity, Similarity}
import org.apache.lucene.store.{NIOFSDirectory, Directory, FSDirectory}
import java.io.File
import org.dbpedia.spotlight.lucene.search.{LuceneCandidateSearcher, MergedOccurrencesContextSearcher}
import org.dbpedia.spotlight.disambiguate.{RandomDisambiguator, Disambiguator}

/**
 * Object with convenience methods to create disambiguators we will use during evaluation
 *
 * @author pablomendes
 */

object DisambiguatorFactory {

    def createMergedDisambiguator(outputFileName: String, analyzer: Analyzer, similarity: Similarity) : Disambiguator = {
        val directory = FSDirectory.open(new File(outputFileName));//+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
        createMergedDisambiguator(outputFileName, analyzer, similarity, directory)
    }

    def createMergedDisambiguator(outputFileName: String, analyzer: Analyzer, similarity: Similarity, directory: Directory) : Disambiguator = {
        createDisambiguator(outputFileName, analyzer, similarity, directory, new MergedOccurrencesDisambiguator(_))
    }

    def createDisambiguator(outputFileName: String, analyzer: Analyzer, similarity: Similarity, directory: Directory, dis: (MergedOccurrencesContextSearcher => Disambiguator)) : Disambiguator = {

        //ensureExists(directory)

        //val luceneManager = new LuceneManager.BufferedMerging(directory)
        val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
        //val isfLuceneManager = new LuceneManager.BufferedMerging(new RAMDirectory())

//        val queryTimeAnalyzer = new QueryAutoStopWordAnalyzer(Version.LUCENE_36, analyzer);
        val queryTimeAnalyzer = analyzer;

        luceneManager.setDefaultAnalyzer(queryTimeAnalyzer);
        luceneManager.setContextSimilarity(similarity);
        //------------ ICF DISAMBIGUATOR
        val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);

        //timed(printTime("Adding auto stopwords took ")) {
        //  queryTimeAnalyzer.addStopWords(contextSearcher.getIndexReader, DBpediaResourceField.CONTEXT.toString, 0.5f);
        //}

        //timed(printTime("Warm up took ")) {
        //  contextSearcher.warmUp(10000);
        //}

        SpotlightLog.info(this.getClass, "Number of entries in merged resource index (%s): %d", contextSearcher.getClass, contextSearcher.getNumberOfEntries)
        // The Disambiguator chooses the best URI for a surface form
        dis(contextSearcher)
    }

    def getNewStopwordedDisambiguator(indexDir: String) : Disambiguator = {
        val f = new File("e:\\dbpa\\data\\surface_forms\\stopwords.list")
        //val stopwords = Source.fromFile(f, "UTF-8").getLines.toSet
        val stopwords = StopAnalyzer.ENGLISH_STOP_WORDS_SET
        println("Stopwords loaded: "+stopwords.size);
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", stopwords);
        val similarity : Similarity = new InvCandFreqSimilarity();
        //val directory =  LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
        val directory =  LuceneManager.pickDirectory(new File(indexDir));
        createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }

    def getICFSnowballDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new InvCandFreqSimilarity();
        //val directory =  LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
        val directory =  LuceneManager.pickDirectory(new File(indexDir));
        createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }

    def getICFCachedDisambiguator(indexDir: String) : Disambiguator = {
      val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      //val directory = LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
      val directory =  LuceneManager.pickDirectory(new File(indexDir));
      val cache = JCSTermCache.getInstance(new LuceneManager.BufferedMerging(directory), 5000);
      val similarity : Similarity = new CachedInvCandFreqSimilarity(cache);
      createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }

    def getICFCachedMixedDisambiguator(indexDir: String) : Disambiguator = {
      val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      //val directory = LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
      val directory =  LuceneManager.pickDirectory(new File(indexDir));
      val cache = JCSTermCache.getInstance(new LuceneManager.BufferedMerging(directory),5000);
      //val similarity : Similarity = new CachedInvCandFreqSimilarity(cache);
      val similarity : Similarity = new InvCandFreqSimilarity
      val mixture = new LinearRegressionMixture
      createDisambiguator(indexDir, analyzer, similarity, directory, new MixedWeightsDisambiguator(_, mixture))
    }

    def getNewDisambiguator(indexDir: String) : Disambiguator = {
      val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      val directory = LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
      val cache = JCSTermCache.getInstance(new LuceneManager.BufferedMerging(directory),5000);
      val similarity : Similarity = new NewSimilarity(cache);
      createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }


    def getICFStandardDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_36, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new InvCandFreqSimilarity();
        val directory = FSDirectory.open(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
        createMergedDisambiguator(indexDir, analyzer, similarity, directory)
    }

    def getDefaultSnowballDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity();
        createMergedDisambiguator(indexDir, analyzer, similarity)
    }

    def getDefaultStandardDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_36, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity();
        createMergedDisambiguator(indexDir, analyzer, similarity)
    }

    def getSweetSpotSnowballDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new SweetSpotSimilarity()
        createMergedDisambiguator(indexDir, analyzer, similarity)
    }

    def getSweetSpotStandardDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_36, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new SweetSpotSimilarity()
        createMergedDisambiguator(indexDir, analyzer, similarity)
    }

    // the next two use an own disambiguator, while the two before just use a different similarity class

//    def getDefaultScorePlusPriorSnowballDisambiguator(indexDir: String) : Disambiguator = {
//        var analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
//        var similarity : Similarity = new DefaultSimilarity();
//        val directory = FSDirectory.open(new File(indexDir+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
//
//        ensureExists(directory)
//        val luceneManager = new LuceneManager.BufferedMerging(directory)
//        //val isfLuceneManager = new LuceneManager.BufferedMerging(new RAMDirectory())
//        luceneManager.setDefaultAnalyzer(analyzer);
//        luceneManager.setContextSimilarity(similarity);
//        //------------ ICF DISAMBIGUATOR
//        val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);
//        SpotlightLog.info(this.getClass, "Number of entries in merged resource index (%s): %d", contextSearcher.getClass, contextSearcher.getNumberOfEntries)
//        // The Disambiguator chooses the best URI for a surface form
//        new MergedPlusPriorDisambiguator(contextSearcher)
//    }







//    def getICFIDFSnowballDisambiguator(indexDir: String) : Disambiguator = {
//        var analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
//        var similarity : Similarity = new ICFIDFSimilarity();
//        createMergedDisambiguator(indexDir, analyzer, similarity, FSDirectory.open(new File(indexDir+"."+analyzer.getClass.getSimpleName+".InvSenseFreqSimilarity")))
//    }
//
//    def getICFIDFStandardDisambiguator(indexDir: String) : Disambiguator = {
//        var analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_36, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
//        var similarity : Similarity = new ICFIDFSimilarity();
//        createMergedDisambiguator(indexDir, analyzer, similarity, FSDirectory.open(new File(indexDir+"."+analyzer.getClass.getSimpleName+".InvSenseFreqSimilarity")))
//    }

    def getPriorDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity
        val directory = new NIOFSDirectory(new File(indexDir));//+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
        //val luceneManager = new LuceneManager.BufferedMerging(directory)
        val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
        luceneManager.setDefaultAnalyzer(analyzer);
        luceneManager.setContextSimilarity(similarity);
        val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);
        SpotlightLog.info(this.getClass, "Number of entries in merged resource index (%s): %d", contextSearcher.getClass, contextSearcher.getNumberOfEntries)
        new LucenePriorDisambiguator(contextSearcher)
    }

    def getRandomDisambiguator(indexDir: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_36, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity
        val directory = new NIOFSDirectory(new File(indexDir));//+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
        //val luceneManager = new LuceneManager.BufferedMerging(directory)
        val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
        luceneManager.setDefaultAnalyzer(analyzer);
        luceneManager.setContextSimilarity(similarity);
        val contextSearcher = new LuceneCandidateSearcher(luceneManager, false);
        SpotlightLog.info(this.getClass, "Number of entries in merged resource index (%s): %d", contextSearcher.getClass, contextSearcher.getNumberOfEntries)
        new RandomDisambiguator(contextSearcher)
    }

}
