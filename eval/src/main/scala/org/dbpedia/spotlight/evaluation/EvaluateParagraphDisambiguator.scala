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
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.disambiguate._
import java.io.{PrintWriter, File}
import org.dbpedia.spotlight.corpus.{PredoseCorpus, MilneWittenCorpus, AidaCorpus}

import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.filter.occurrences.{UriWhitelistFilter, RedirectResolveFilter, OccurrenceFilter}
import scala.Some




/**
 * Evaluation for disambiguators that take one paragraph at a time, instead of one occurrence at a time.
 *
 * @author pablomendes
 */
object EvaluateParagraphDisambiguator {

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
        val mrrResults = paragraphs.map(a => {
            i = i + 1
            SpotlightLog.info(this.getClass, "Paragraph %d/%d: %s.", i, totalParagraphs, a.id)
            val paragraph = Factory.Paragraph.from(a)
            nOriginalOccurrences = nOriginalOccurrences + a.occurrences.toTraversable.size

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
                        nZeros = nZeros + 1
                    } else if (disambResult.rank==1)  {
                        nCorrects = nCorrects + 1
                    }
                    acc = acc + invRank
                });
                outputs.foreach(_.flush)
            } catch {
                case e: Exception => SpotlightLog.error(this.getClass, "%s\n%s", e.getMessage, e.getStackTraceString)
            }
            val mrr = if (a.occurrences.size==0) 0.0 else acc / a.occurrences.size
            SpotlightLog.info(this.getClass, "Mean Reciprocal Rank (MRR) = %.5f", mrr)
            mrr
        })
        val endTime = System.nanoTime()
        SpotlightLog.info(this.getClass, "********************")
        SpotlightLog.info(this.getClass, "Corpus: %s", testSource.name)
        SpotlightLog.info(this.getClass, "Number of occs: %d (original), %d (processed)", nOriginalOccurrences,nOccurrences)
        SpotlightLog.info(this.getClass, "Disambiguator: %s", disambiguator.name)
        SpotlightLog.info(this.getClass, "Correct URI not found = %d / %d = %.3f", nZeros,nOccurrences,nZeros.toDouble/nOccurrences)
        SpotlightLog.info(this.getClass, "Accuracy = %d / %d = %.3f", nCorrects,nOccurrences,nCorrects.toDouble/nOccurrences)
        SpotlightLog.info(this.getClass, "Global MRR: %s", mrrResults.sum / mrrResults.size)
        SpotlightLog.info(this.getClass, "Elapsed time: %s sec", (endTime-startTime) / 1000000000)
        SpotlightLog.info(this.getClass, "********************")

        val disambigSummary = "Corpus: %s".format(testSource.name) +
                    "\nNumber of occs: %d (original), %d (processed)".format(nOriginalOccurrences,nOccurrences) +
                    "\nDisambiguator: %s".format(disambiguator.name)+
                    "\nCorrect URI not found = %d / %d = %.3f".format(nZeros,nOccurrences,nZeros.toDouble/nOccurrences)+
                    "\nAccuracy = %d / %d = %.3f".format(nCorrects,nOccurrences,nCorrects.toDouble/nOccurrences) +
                    "\nGlobal MRR: %s".format(mrrResults.sum / mrrResults.size)+
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

        val redirectTCFileName  = if (args.size>1) args(1) else "data/redirects_tc.tsv" //produced by ExtractCandidateMap
        val conceptURIsFileName  = if (args.size>2) args(2) else "data/conceptURIs.list" //produced by ExtractCandidateMap

        //val default : Disambiguator = new DefaultDisambiguator(config)
        //val test : Disambiguator = new GraphCentralityDisambiguator(config)

        val factory = new SpotlightFactory(config)

        //val topics = HashMapTopicalPriorStore.fromDir(new File("data/topics"))
        val disambiguators = Set(//new TopicalDisambiguator(factory.candidateSearcher,topics),
                                 //new TopicBiasedDisambiguator(factory.candidateSearcher,factory.contextSearcher,topics)
                                 new TwoStepDisambiguator(factory.candidateSearcher,factory.contextSearcher)
                                 //, new CuttingEdgeDisambiguator(factory),
                                 //new PageRankDisambiguator(factory)
                                )

        val sources = List(//AidaCorpus.fromFile(new File("/home/pablo/eval/aida/gold/CoNLL-YAGO.tsv")),
                           //MilneWittenCorpus.fromDirectory(new File("/home/pablo/eval/wikify/original"))
                           PredoseCorpus.fromFile(new File("/home/pablo/eval/predose/predose_annotations.tsv")))

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

        sources.foreach( paragraphs => {
          val testSourceName = paragraphs.name
          disambiguators.foreach( d => {
              val dName = d.name.replaceAll("""[.*[?/<>|*:\"{\\}].*]""","_")
              val tsvOut = new TSVOutputGenerator(new PrintWriter("%s-%s-%s.pareval.log".format(testSourceName,dName,EvalUtils.now())))
              //val arffOut = new TrainingDataOutputGenerator()
              val outputs = List(tsvOut)
              evaluate(paragraphs, d, outputs, occFilters)
              outputs.foreach(_.close)
          })
        })
    }
}
