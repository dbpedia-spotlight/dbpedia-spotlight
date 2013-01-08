/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.io._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.disambiguate._
import java.io.{PrintWriter, File}
import org.dbpedia.spotlight.corpus._

import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.filter.occurrences.{UriWhitelistFilter, RedirectResolveFilter, OccurrenceFilter}
import scala.Some

/**
 * Evaluation for disambiguators that take one paragraph at a time, instead of one occurrence at a time.
 *
 * @author pablomendes
 */
object EvaluateParagraphDisambiguator {

    private val LOG = LogFactory.getLog(this.getClass)

    def filter(bestK: Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]) :  Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {
        bestK;
    }

    def evaluate(testSource: AnnotatedTextSource, disambiguator: ParagraphDisambiguator, outputs: List[OutputGenerator], occFilters: List[OccurrenceFilter] ) {
        val startTime = System.nanoTime()

        var i = 0;
        var nZeros = 0
        var nCorrects = 0
        var nOccurrences = 0
        var nOriginalOccurrences = 0
        val paragraphs = testSource.toList
        var totalParagraphs = paragraphs.size

        //testSource.view(10000,15000)
        val (mrrResults,mrrResultsWithoutZeros) = paragraphs.foldLeft( (List[Double](), List[Double]()) ) { case  ((mrrResults, mrrResultsWithoutZeros), a) => {
            i = i + 1
            LOG.info("Paragraph %d/%d: %s.".format(i, totalParagraphs, a.id))
            val paragraph = Factory.Paragraph.from(a)
            nOriginalOccurrences = nOriginalOccurrences + a.occurrences.toTraversable.size
            var zeros = 0
            var acc = 0.0
            try {
                val bestK = filter(disambiguator.bestK(paragraph,100))

                val goldOccurrences = occFilters.foldLeft(a.occurrences.toTraversable){ (o,f) => f.filterOccs(o) } // discounting URIs from gold standard that we know are disambiguations, fixing redirects, etc.

                goldOccurrences.foreach( correctOccurrence => {
                    nOccurrences = nOccurrences + 1

                    val disambResult = new DisambiguationResult(correctOccurrence,                                                     // correct
                                                                bestK.getOrElse(Factory.SurfaceFormOccurrence.from(correctOccurrence), // predicted
                                                                                List[DBpediaResourceOccurrence]()))

                    outputs.foreach(_.write(disambResult))

                    val invRank = if (disambResult.rank>0) (1.0/disambResult.rank) else  0.0
                    if (disambResult.rank==0)  {
                        zeros = zeros + 1
                    } else if (disambResult.rank==1)  {
                        nCorrects = nCorrects + 1
                    }
                    acc = acc + invRank
                });
                outputs.foreach(_.flush)
            } catch {
                case e: Exception => LOG.error(e.printStackTrace())
            }

            nZeros += zeros
            val mrr = if (a.occurrences.size==0) 0.0 else acc / a.occurrences.size
            val mrrWithoutZero = if (a.occurrences.size - zeros==0) 0.0 else acc / (a.occurrences.size - zeros)

            LOG.info("Mean Reciprocal Rank (MRR) = %.5f".format(mrr))

            (mrrResults.::(mrr),
             mrrResultsWithoutZeros.::(mrrWithoutZero) )
        }}
        val endTime = System.nanoTime()
        LOG.info("********************")
        LOG.info("Corpus: %s".format(testSource.name))
        LOG.info("Number of occs: %d (original), %d (processed)".format(nOriginalOccurrences,nOccurrences))
        LOG.info("Disambiguator: %s".format(disambiguator.name))
        LOG.info("Correct URI not found = %d / %d = %.3f".format(nZeros,nOccurrences,nZeros.toDouble/nOccurrences))
        LOG.info("Accuracy = %d / %d = %.3f".format(nCorrects,nOccurrences,nCorrects.toDouble/nOccurrences))
        LOG.info("Global MRR: %s".format(mrrResults.sum / mrrResults.size))
        LOG.info("Global MRR without zeros: %s".format(mrrResultsWithoutZeros.sum /  mrrResults.size))
        LOG.info("Elapsed time: %s sec".format( (endTime-startTime) / 1000000000))
        LOG.info("********************")

        val disambigSummary = "Corpus: %s".format(testSource.name) +
                    "\nNumber of occs: %d (original), %d (processed)".format(nOriginalOccurrences,nOccurrences) +
                    "\nDisambiguator: %s".format(disambiguator.name)+
                    "\nCorrect URI not found = %d / %d = %.3f".format(nZeros,nOccurrences,nZeros.toDouble/nOccurrences)+
                    "\nAccuracy = %d / %d = %.3f".format(nCorrects,nOccurrences,nCorrects.toDouble/nOccurrences) +
                    "\nGlobal MRR: %s".format(mrrResults.sum / mrrResults.size)+
                    "\nGlobal MRR without zeros: %s".format(mrrResultsWithoutZeros.sum / mrrResults.size)+
                    "\nElapsed time: %s sec".format( (endTime-startTime) / 1000000000);

        outputs.foreach(_.summary(disambigSummary))

        outputs.foreach(_.flush)
    }

    def main(args : Array[String]) {
        //val indexDir: String = args(0)  //"e:\\dbpa\\data\\index\\index-that-works\\Index.wikipediaTraining.Merged."
        val config = new SpotlightConfiguration(args(0));

        //val testFileName: String = args(1)  //"e:\\dbpa\\data\\index\\dbpedia36data\\test\\test100k.tsv"
        //val paragraphs = AnnotatedTextSource
        //                    .fromOccurrencesFile(new File(testFileName))

        val redirectTCFileName  = if (args.size>1) args(1) else "D:/dirk/data/Wikipedia/output/redirects_tc.tsv" //produced by ExtractCandidateMap
        val conceptURIsFileName  = if (args.size>2) args(2) else "D:/dirk/data/Wikipedia/output/conceptURIs.list" //produced by ExtractCandidateMap


        val noNils = new OccurrenceFilter {
            def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
                if (occ.id.endsWith("DISAMBIG") || //Max has marked ids with DISAMBIG in a TSV file for one of our datasets
                    occ.resource.uri.equals(AidaCorpus.nilUri) ||
                    occ.resource.uri.startsWith("http://knoesis")) {
                    None
                } else {
                    Some(occ)
                }
            }
        }
        val occFilters = List(UriWhitelistFilter.fromFile(new File(conceptURIsFileName)),
            RedirectResolveFilter.fromFile(new File(redirectTCFileName)),
            noNils)

        //val default : Disambiguator = new DefaultDisambiguator(config)
        //val test : Disambiguator = new GraphCentralityDisambiguator(config)

        val factory = new SpotlightFactory(config)

        val disambiguators = Set(//new TopicalDisambiguator(factory.candidateSearcher,HashMapTopicalPriorStore, factory.topicalClassifier),
                     //new TopicalFilteredDisambiguator(factory.candidateSearcher,factory.contextSearcher,
                     //    HashMapTopicalPriorStore,null, 0.3, factory.topicalClassifier)
                     new TwoStepDisambiguator(factory.candidateSearcher,factory.contextSearcher)
                     //, new CuttingEdgeDisambiguator(factory),
                     //new PageRankDisambiguator(factory)
                    )

        val sources = List(//AidaCorpus.fromFile(new File("/home/pablo/eval/aida/gold/CoNLL-YAGO.tsv")),
                           //new SmallContextOccurrencesCorpus(MilneWittenCorpus.fromDirectory(new File("/home/dirk/Dropbox/eval/wikifiedStories"))) )
                            MilneWittenCorpus.fromDirectory(new File("C:/Users/dirk/Dropbox/eval/wikifiedStories")))
                           //PredoseCorpus.fromFile(new File("/home/pablo/eval/predose/predose_annotations.tsv"))
                           // CSAWCorpus.fromDirectory(new File("/home/dirk/eval/corpus/csaw")))

        sources.foreach( paragraphs => {
          val testSourceName = paragraphs.name
          disambiguators.foreach( d => {
              val dName = d.name.replaceAll("""[.*[?/<>|*:\"{\\}].*]""","_")
              //val arffOut = new TrainingDataOutputGenerator()
              val outputs = List(   new ProbabilityTrainingData(new PrintWriter("%s-%s-%s.pareval.log".format(testSourceName,dName,EvalUtils.now())))    )

                  //new TopicalScoreDataGenerator(new PrintWriter("%s-%s-%s.scores.pareval.log".format(testSourceName,dName,EvalUtils.now()))),
                  //                  new TopicalFilterDataGenerator(new PrintWriter("%s-%s-%s.filter.pareval.log".format(testSourceName,dName,EvalUtils.now()))))
              evaluate(paragraphs, d, outputs, occFilters)
              outputs.foreach(_.close)
          })
        })
    }
}
