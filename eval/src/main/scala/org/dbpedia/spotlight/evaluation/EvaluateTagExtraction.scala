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

package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.log.SpotlightLog
import io.Source
import org.dbpedia.spotlight.util.AnnotationFilter
import org.dbpedia.spotlight.string.WikiLinkParser
import scala.collection.JavaConversions._
import java.io.{PrintStream, File}
import org.dbpedia.spotlight.model.{SpotlightFactory, SpotlightConfiguration, DBpediaResource, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.annotate.DefaultParagraphAnnotator
import org.dbpedia.spotlight.disambiguate.{ParagraphDisambiguatorJ, TwoStepDisambiguator}
import org.dbpedia.spotlight.extract.TagExtractorFromAnnotator

/**
 * Reads in text from evaluation corpus, for each text item produces a list of ranked tags
 * Uses spotters and disambiguators to find the tags. This is an adaptation of the annotation task to extract tags.
 * For optimal results in tag extraction, we should implement customized classes for that task.
 *
 * @author pablomendes
 */
object EvaluateTagExtraction
{
    val configuration = new SpotlightConfiguration("conf/eval.properties");
//    val confidence = 0.0;
//    val support = 0;
    val factory = new SpotlightFactory(configuration)
    val disambiguator = new ParagraphDisambiguatorJ(new TwoStepDisambiguator(factory.candidateSearcher, factory.contextSearcher))
//  val disambiguator = new ParagraphDisambiguatorJ(new TwoStepDisambiguator(factory))

  val spotter = factory.spotter()

    val targetTypesList = null; //TODO filter by type
    val coreferenceResolution = true;

    val annotator = new DefaultParagraphAnnotator(spotter, disambiguator);
    val filter = new AnnotationFilter(configuration)

    //val randomBaseline = new DefaultAnnotator(spotter, new RandomDisambiguator(contextSearcher))

//    val tfidfLuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
//    tfidfLuceneManager.setContextSimilarity(new DefaultSimilarity())
//    tfidfLuceneManager.setDefaultAnalyzer(analyzer);
//    val tfidfBaseline  = new DefaultAnnotator(spotter, spotSelector, new MergedOccurrencesDisambiguator(new MergedOccurrencesContextSearcher(tfidfLuceneManager)))

//    val factory = new LuceneFactory(configuration)
//    val annotator = factory.annotator()
//    val filter = factory.filter()

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

//        val baseDir: String = "/home/pablo/eval/csaw/"
//        val inputFile: File = new File(baseDir+"gold/paragraphs.txt");

        val baseDir: String = "/home/pablo/eval/bbc/"
        val inputFile: File = new File(baseDir+"gold/transcripts.txt");

        val prefix = "spotlight/Spotlight";

        if (!new File(baseDir).exists) {
            System.err.println("Base directory does not exist. "+baseDir);
            exit();
        }

        //ANNOTATE AND TRANSFORM TO MATRIX

        val plainText = Source.fromFile(inputFile).mkString // Either get from file
        //val plainText = AnnotatedTextParser.eraseMarkup(text)     //   or erase markup


        val setOutputFile: File = new File(baseDir+prefix+"NoFilter.set");

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

        var i = 0;
        for (text <- plainText.split("\n\n")) {
            val cleanText = WikiLinkParser.eraseMarkup(text);

            //TODO run baselines
            //val baselineOcc = randomBaseline.annotate(cleanText).toList
            //randomBaselineResults.append("\n"+baselineOcc.map(o => o.resource.uri).toSet.mkString("\n")+"\n")

            i = i + 1
            var occs = List[DBpediaResourceOccurrence]()
            try {
                SpotlightLog.info(this.getClass, "Doc %d", i)
                SpotlightLog.info(this.getClass, "Doc length: %s tokens", cleanText.split(" ").size)
                occs = annotator.annotate(cleanText).toList;
            } catch {
                case e: Exception =>
                    SpotlightLog.error(this.getClass, "Exception: %s", e)
            }

            val allEntities = TagExtractorFromAnnotator.bySimilarity(annotator).rank(occs)
            append(allOut, allEntities)

            val sixPercent = (cleanText.split("\\s+").size * 0.06).round.toInt;
            //val sixPercent = 20;
            val k = Math.min(occs.size, sixPercent)

            val entitiesByScore = TagExtractorFromAnnotator.bySimilarity(annotator).rank(occs)
            append(top10Score, entitiesByScore.slice(0,k))

            val entitiesByConfusion = TagExtractorFromAnnotator.byConfusion(annotator).rank(occs); //should not be reversed. small percentage is large gap.
            append(top10Confusion,entitiesByConfusion.slice(0,k))

            val entitiesByPrior = TagExtractorFromAnnotator.byPrior(annotator).rank(occs)
            append(top10Prior,entitiesByPrior.slice(0,k))

            val entitiesByConfidence = TagExtractorFromAnnotator.byConfidence(annotator).rank(occs)
            append(top10Confidence,entitiesByConfidence.slice(0,k))

            val entitiesByContext = TagExtractorFromAnnotator.byContext(annotator).rank(occs)
            append(top10Context,entitiesByContext.slice(0,k))

            EvalUtils.writeResultsForIntervals(baseDir, prefix, occs, "doc"+i, configuration.getSimilarityThresholds.map(_.doubleValue).toList)

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


    def append(stream: PrintStream, tags: List[(DBpediaResource,Double)]) {
        stream.append("\n"+tags.map(_._1).mkString("\n")+"\n")
    }

}