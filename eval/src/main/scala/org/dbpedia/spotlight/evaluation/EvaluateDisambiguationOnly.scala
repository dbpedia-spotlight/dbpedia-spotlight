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

import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.io.{FileOccurrenceSource}
import org.dbpedia.spotlight.log.SpotlightLog
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
import org.dbpedia.spotlight.lucene._
import search.{LuceneCandidateSearcher, MergedOccurrencesContextSearcher}
import org.dbpedia.spotlight.model.{SpotlightFactory, DBpediaResource, SpotlightConfiguration, ContextSearcher}
import org.dbpedia.spotlight.corpus.{MilneWittenCorpus, AidaCorpus}

/**
 * This class evaluates the micro-averaged accuracy of disambiguation (same measure used in TAC KBP 2010).

 * It also produces a log of every disambiguation, with many parameters to allow retraining
 *
 * Usage:
 *  mvn scala:run -DmainClass=org.dbpedia.spotlight.evaluation.EvalDisambiguationOnly ../conf/server.properties output/occs.uriSorted.tsv
 *
 * Optionally you may want to get a random sample of paragraphs instead of running on all 60M
 *  sort -R --random-source=/dev/urandom output/occs.uriSorted.tsv
 *  split -l 1000000 occs.uriSorted.tsv occs.uriSorted.tsv.split
 *
 * TODO Move create*Disambiguator methods to a factory
 *
 * @author maxjakob
 */
object EvaluateDisambiguationOnly
{

    def exists(fileName: String) {
        if (!new File(fileName).exists) {
            System.err.println("Important file/dir does not exist. "+fileName);
            exit(1);
        }
    }

    def main(args : Array[String])
    {

        //val indexDir: String = args(0)  //"e:\\dbpa\\data\\index\\index-that-works\\Index.wikipediaTraining.Merged."
        val config = new SpotlightConfiguration(args(0));
        val indexDir = config.getContextIndexDirectory

        val simScoresFileName: String = ""

//        val testFileName: String = args(1)  //"e:\\dbpa\\data\\index\\dbpedia36data\\test\\test100k.tsv"
//        val resultsFileName: String = testFileName+".log"


        //val out = new PrintStream(resultsFileName, "UTF-8");

        //exists(indexDir);
//        exists(testFileName);
        //exists(resultsFileName);

        val osName : String = System.getProperty("os.name");
        SpotlightLog.info(this.getClass, "Your operating system is: %s", osName)

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

        val factory = new SpotlightFactory(config)
        val default : Disambiguator = new DefaultDisambiguator(factory.contextSearcher)
        val cuttingEdge : Disambiguator = new CuttingEdgeDisambiguator(factory.contextSearcher)

        //val test : Disambiguator = new GraphCentralityDisambiguator(config)
        val disSet = Set(cuttingEdge);

        /*val disSet = Set(

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
         */

        // Read some text to test.
        //val testSource = FileOccurrenceSource.fromFile(new File(testFileName))
            //.filterNot(o => o.resource.uri == "NIL")
           //.filterNot(o => o.id.endsWith("DISAMBIG"))

        val sources = List(AidaCorpus.fromFile(new File("/home/pablo/eval/aida/gold/CoNLL-YAGO.tsv")).flatMap(p => p.occurrences).filterNot(o => o.resource.uri equals (AidaCorpus.nilUri)),
                           MilneWittenCorpus.fromDirectory(new File("/home/pablo/eval/wikify/original")).flatMap(p => p.occurrences))


        sources.foreach( testSource => {
            val resultsFileName = "data/"+testSource.getClass.getSimpleName.trim
            //testSource.view(10000,15000)
            //testSource.filter(o => o.surfaceForm.name.toLowerCase.equals("on"))
            //testSource.filter(o => o.surfaceForm.name.toLowerCase.startsWith("the"))
            val evaluator = new DisambiguationEvaluator(testSource, disSet, resultsFileName);
            evaluator.ambiguousOnly = false;

            evaluator.evaluate(1)
        });


    }

}
