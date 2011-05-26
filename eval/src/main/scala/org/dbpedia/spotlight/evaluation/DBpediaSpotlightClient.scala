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

import org.apache.commons.logging.LogFactory
import io.Source
import org.dbpedia.spotlight.annotate.DefaultAnnotator
import org.dbpedia.spotlight.util.AnnotationFilter
import org.dbpedia.spotlight.string.WikiLinkParser
import scala.collection.JavaConversions._
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import org.dbpedia.spotlight.disambiguate.mixtures.LinearRegressionMixture
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache, InvCandFreqSimilarity}
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import java.io.{FileOutputStream, PrintStream, File}
import org.dbpedia.spotlight.model.{LuceneFactory, SpotlightConfiguration, DBpediaResource, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.candidate.{AtLeastOneNounFilter, CommonWordFilter, SpotSelector}
import org.dbpedia.spotlight.disambiguate.RandomDisambiguator
import org.dbpedia.spotlight.filter.annotations.{ContextualScoreFilter, SupportFilter, CoreferenceFilter, ConfidenceFilter}
import org.dbpedia.spotlight.lucene.disambiguate.{MergedOccurrencesDisambiguator, MixedWeightsDisambiguator}
import org.apache.lucene.search.{DefaultSimilarity, Similarity}

/**
 * Reads in manually annotated paragraphs, computes the inter-annotator agreement, then compares
 * our system against the union or intersection of the manual annotators.
 *
 * TODO Create client for Spotlight in the same style created for Alchemy, Ontos, OpenCalais, WikiMachine and Zemanta. i.e. using the Web Service.
 *
 * @author pablomendes
 */
object DBpediaSpotlightClient
{
    private val LOG = LogFactory.getLog(this.getClass)

    val configuration = new SpotlightConfiguration("conf/eval.properties");
//    val confidence = 0.0;
//    val support = 0;

    val targetTypesList = null;
    val coreferenceResolution = true;

    val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
    //val directory = LuceneManager.pickDirectory(new File(indexDir+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
    val directory =  LuceneManager.pickDirectory(new File(configuration.getIndexDirectory));
    val cache = new JCSTermCache(new LuceneManager.BufferedMerging(directory), configuration.getMaxCacheSize);
    val similarity : Similarity = new CachedInvCandFreqSimilarity(cache);
    //val similarity : Similarity = new InvCandFreqSimilarity
    val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
    luceneManager.setContextAnalyzer(analyzer);
    luceneManager.setContextSimilarity(similarity);
    val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);

    val spotter = new LingPipeSpotter(new File(configuration.getSpotterFile));
    val spotSelector: SpotSelector = null//new AtLeastOneNounFilter(configuration.getTaggerFile)
    val mixture = new LinearRegressionMixture
    val disambiguator = new MixedWeightsDisambiguator(contextSearcher,mixture);
    val annotator = new DefaultAnnotator(spotter, spotSelector, disambiguator);
    val filter = new AnnotationFilter(configuration)

    val randomBaseline = new DefaultAnnotator(spotter, spotSelector, new RandomDisambiguator(contextSearcher))

//    val tfidfLuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
//    tfidfLuceneManager.setContextSimilarity(new DefaultSimilarity())
//    tfidfLuceneManager.setContextAnalyzer(analyzer);
//    val tfidfBaseline  = new DefaultAnnotator(spotter, spotSelector, new MergedOccurrencesDisambiguator(new MergedOccurrencesContextSearcher(tfidfLuceneManager)))

//    val factory = new LuceneFactory(configuration)
//    val annotator = factory.annotator()
//    val filter = factory.filter()

    def parseToMatrix(occList : List[DBpediaResourceOccurrence]) : String = {
        val buffer = new StringBuffer();
        var pos = 0;
        var text = "";
        for (occ <- occList) {
            text = occ.context.text;
            val chunk = occ.context.text.substring(pos, occ.textOffset);
            WikiLinkParser.appendToMatrix(chunk, new DBpediaResource(WikiLinkParser.NoTag), buffer);
            WikiLinkParser.appendToMatrix(occ.surfaceForm.name, occ.resource, buffer);
            pos = occ.textOffset + occ.surfaceForm.name.length();
        }
        WikiLinkParser.appendToMatrix(text.substring(pos),new DBpediaResource(WikiLinkParser.NoTag),buffer);
        return buffer.toString();
    }

    def writeResultsForIntervals(baseDir: String, prefix: String, occurrences: List[DBpediaResourceOccurrence]) {
        //TODO What happens here if we give all occurrences from different texts?
        val filteredOccList = new CoreferenceFilter().filterOccs(occurrences)

        for (confidence <- EvalParams.confidenceInterval) {
            val confidenceFilter = new ConfidenceFilter(configuration.getSimilarityThresholds.map(_.doubleValue).toList, confidence)
            for(support <- EvalParams.supportInterval) {
                val supportFilter = new SupportFilter(support)

                //var localFiltered = filter.filterBySupport(filteredOccList, support)
                //localFiltered = filter.filterByConfidence(localFiltered, confidence)
                var localFiltered = supportFilter.filterOccs(filteredOccList)
                localFiltered = confidenceFilter.filterOccs(localFiltered)

                val out = new PrintStream(new FileOutputStream(baseDir+prefix+".c"+confidence+"s"+support+".set", true))
                out.append("\n"+localFiltered.map(occ => occ.resource.uri).toSet.mkString("\n")+"\n")
                out.close();
            }
        }

        for (score <- EvalParams.contextualScoreInterval) { //TODO make it contextual score by prior
            val confidenceFilter = new ContextualScoreFilter(score)
            for(support <- EvalParams.supportInterval) {
                val supportFilter = new SupportFilter(support)

                var localFiltered = supportFilter.filterOccs(filteredOccList)
                localFiltered = confidenceFilter.filterOccs(localFiltered)

                val out = new PrintStream(new FileOutputStream(baseDir+prefix+".s"+score+"p"+support+".set", true))
                out.append("\n"+localFiltered.map(occ => occ.resource.uri).toSet.mkString("\n")+"\n")
                out.close();
            }
        }
    }

    def main(args : Array[String])
    {

        //      val baseDir: String = "/home/pablo/eval/manual"
        //      val inputFile: File = new File(baseDir+"AnnotationText.txt");
        //      val prefix = "spotlight/AnnotationText-Spotlight";
        //      val outputFile: File = new File(baseDir+prefix+".txt.matrix");

        //      val baseDir: String = "/home/pablo/eval/cucerzan/"
        //      val inputFile: File = new File(baseDir+"cucerzan.txt");

//                val baseDir: String = "/home/pablo/eval/manual/"
//                val inputFile: File = new File(baseDir+"AnnotationText.txt");

//        val baseDir: String = "/home/pablo/eval/wikify/"
//        val inputFile: File = new File(baseDir+"gold/WikifyAllInOne.txt");

//        val baseDir: String = "/home/pablo/eval/grounder/"
//        val inputFile: File = new File(baseDir+"gold/g1b_spotlight.txt");

        val baseDir: String = "/home/pablo/eval/csaw/"
        val inputFile: File = new File(baseDir+"gold/paragraphs.txt");

        val prefix = "spotlight/Spotlight";
        val setOutputFile: File = new File(baseDir+prefix+"NoFilter.set");
        val matrixOutputFile = new File(baseDir+prefix+".matrix");
        if (!new File(baseDir).exists) {
            System.err.println("Base directory does not exist. "+baseDir);
            exit();
        }

        //ANNOTATE AND TRANSFORM TO MATRIX

        val plainText = Source.fromFile(inputFile).mkString // Either get from file
        //val plainText = AnnotatedTextParser.eraseMarkup(text)     //   or erase markup


        // Cleanup last run.
        for (confidence <- EvalParams.confidenceInterval) {
            for(support <- EvalParams.supportInterval) {
                new File(baseDir+prefix+".c"+confidence+"s"+support+".set").delete
            }
        }

        val allOut = new PrintStream(setOutputFile)

        val randomBaselineResults = new PrintStream(baseDir+prefix+"Random.set")

        val top10Score = new PrintStream(baseDir+prefix+"Top10Score.set");
        val top10Confusion = new PrintStream(baseDir+prefix+"Top10Confusion.set");
        val top10Prior = new PrintStream(baseDir+prefix+"Top10Prior.set");
        val top10Confidence = new PrintStream(baseDir+prefix+"Top10Confidence.set");
        val top10Context = new PrintStream(baseDir+prefix+"Top10Context.set");
        //val out = new PrintStream(matrixOutputFile);

        for (text <- plainText.split("\n\n")) {
            val cleanText = WikiLinkParser.eraseMarkup(text);

            val baselineOcc = randomBaseline.annotate(cleanText).toList
            randomBaselineResults.append("\n"+baselineOcc.map(o => o.resource.uri).toSet.mkString("\n")+"\n")

            val occ = annotator.annotate(cleanText).toList;

            allOut.append("\n"+occ.map(o => o.resource.uri).toSet.mkString("\n")+"\n")

            val sixPercent = (cleanText.split("\\s+").size * 0.06).round.toInt;
            //val sixPercent = 20;
            val k = Math.min(occ.size, sixPercent)

            val entitiesByScore = occ.sortBy(o=>o.similarityScore).reverse.map(o=>o.resource.uri);
            top10Score.append("\n"+entitiesByScore.slice(0,k).toSet.mkString("\n")+"\n")

            val entitiesByConfusion = occ.sortBy(o=>o.percentageOfSecondRank).map(o=>o.resource.uri); //should not be reversed. small percentage is large gap.
            top10Confusion.append("\n"+entitiesByConfusion.slice(0,k).toSet.mkString("\n")+"\n")

            val entitiesByPrior = occ.sortBy(o=>o.resource.prior).reverse.map(o=>o.resource.uri);
            top10Prior.append("\n"+entitiesByPrior.slice(0,k).toSet.mkString("\n")+"\n")

            val entitiesByConfidence = occ.sortBy(o=> (o.similarityScore * (1-o.percentageOfSecondRank)) ).reverse.map(o=>o.resource.uri);
            top10Confidence.append("\n"+entitiesByConfidence.slice(0,k).toSet.mkString("\n")+"\n")

            val entitiesByContext = occ.sortBy(o=> o.contextualScore ).reverse.map(o=>o.resource.uri);
            top10Context.append("\n"+entitiesByContext.slice(0,k).toSet.mkString("\n")+"\n")

            writeResultsForIntervals(baseDir, prefix, occ)

        }
        //out.close();
        top10Score.close()
        top10Prior.close()
        top10Confusion.close()
        top10Confidence.close()
        top10Context.close()

        allOut.close();
        randomBaselineResults.close();


        SetEvaluation.run(baseDir)

    }

    def writeAsEntitySetToFile(filteredOccList: java.util.List[DBpediaResourceOccurrence], file: File) {
        val out = new PrintStream(file)
        val entitySet = filteredOccList
        out.append("\n"+entitySet.map(occ => occ.resource.uri).toSet.mkString("\n")+"\n")
        out.close();
    }

    def writeAsEntitySetToFile(occList: List[Tuple2[DBpediaResource,Double]], file: File) {
        val out = new PrintStream(file)
        out.append("\n"+occList.map( t => t._1.uri).toSet.mkString("\n")+"\n")
        out.close();
    }

}