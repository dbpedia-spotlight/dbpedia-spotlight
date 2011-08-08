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

import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.model._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.disambiguate.{CuttingEdgeDisambiguator, TwoStepDisambiguator, ParagraphDisambiguator}
import java.io.{PrintWriter, File}

/**
 * Evaluation for disambiguators that take one paragraph at a time, instead of one occurrence at a time.
 *
 * @author pablomendes
 */
object EvaluateParagraphDisambiguator {

    private val LOG = LogFactory.getLog(this.getClass)

    def getRank(correctOccurrence: DBpediaResourceOccurrence, bestK: List[DBpediaResourceOccurrence]) = {
        LOG.debug("Ranking for: %s -> %s".format(correctOccurrence.surfaceForm,correctOccurrence.resource))
        LOG.debug("K=%s".format(bestK.size));
        var rank,i = 0
        //LOG.debug("                : prior \t context \t final \t uri")
        LOG.debug("                : context \t uri")
        for(predictedOccurrence <- bestK) {
            i = i + 1
            if(correctOccurrence.resource equals predictedOccurrence.resource) {
                rank = i
                //LOG.debug("  **     correct: %.5f \t %.5f \t %.5f \t %s".format(predictedOccurrence.resource.prior, predictedOccurrence.contextualScore, predictedOccurrence.similarityScore, predictedOccurrence.resource))
                LOG.debug("  **     correct: %.5f \t %s".format(predictedOccurrence.contextualScore, predictedOccurrence.resource))
            }
            else {
                //LOG.debug("       spotlight: %.5f \t %.5f \t %.5f \t %s".format(predictedOccurrence.resource.prior, predictedOccurrence.contextualScore, predictedOccurrence.similarityScore, predictedOccurrence.resource))
                LOG.debug("       spotlight: %.5f \t %s".format(predictedOccurrence.contextualScore, predictedOccurrence.resource))
            }
        }
        if (rank==0)
            LOG.debug("  **   not found: %.5s \t %.5s \t %.5s \t %s".format("NA", "NA", "NA", correctOccurrence.resource))
        LOG.debug("Rank: %s".format(rank))
        rank
    }

    def evaluate(testSource: Traversable[AnnotatedParagraph], disambiguator: ParagraphDisambiguator, output: PrintWriter) {
        val startTime = System.nanoTime()

        var i = 0;
        var nZeros = 0
        var nCorrects = 0
        var nOccurrences = 0
        var totalParagraphs = testSource.size
        //testSource.view(10000,15000)
        val mrrResults = testSource.map(a => {
            i = i + 1
            LOG.info("Paragraph %d/%d: %s.".format(i, totalParagraphs, a.id))
            val paragraph = Factory.Paragraph.from(a)
            //LOG.debug(a)
            val bestK = disambiguator.bestK(paragraph,100)
            var acc = 0.0
            a.occurrences
                .filterNot(o => o.id.endsWith("DISAMBIG")) // discounting URIs from gold standard that we know are disambiguations
                .foreach( correctOccurrence => {
                nOccurrences = nOccurrences + 1
                val rank = getRank(correctOccurrence,                                                     // correct
                                   bestK.getOrElse(Factory.SurfaceFormOccurrence.from(correctOccurrence), // predicted
                                                   List[DBpediaResourceOccurrence]()))
                val invRank = if (rank>0) (1.0/rank) else  0.0
                if (rank==0)  {
                    nZeros = nZeros + 1
                } else if (rank==1)  {
                    nCorrects = nCorrects + 1
                }
                acc = acc + invRank
            });
            val mrr = acc / a.occurrences.size
            LOG.info("Mean Reciprocal Rank (MRR) = %.5f".format(mrr))
            mrr
        })
        val endTime = System.nanoTime()
        LOG.info("********************")
        LOG.info("Disambiguator: %s".format(disambiguator.name))
        LOG.info("Correct URI not found = %d / %d = %.3f".format(nZeros,nOccurrences,nZeros.toDouble/nOccurrences))
        LOG.info("Accuracy = %d / %d = %.3f".format(nCorrects,nOccurrences,nCorrects.toDouble/nOccurrences))
        LOG.info("Global MRR: %s".format(mrrResults.sum / mrrResults.size))
        LOG.info("Elapsed time: %s sec".format( (endTime-startTime) / 1000000000))
        LOG.info("********************")

        output.append("\n********************" +
            "\nDisambiguator: %s".format(disambiguator.name)+
            "\nCorrect URI not found = %d / %d = %.3f".format(nZeros,nOccurrences,nZeros.toDouble/nOccurrences)+
            "\nAccuracy = %d / %d = %.3f".format(nCorrects,nOccurrences,nCorrects.toDouble/nOccurrences) +
            "\nGlobal MRR: %s".format(mrrResults.sum / mrrResults.size)+
            "\nElapsed time: %s sec".format( (endTime-startTime) / 1000000000))

        output.flush
    }

    def main(args : Array[String]) {
        //val indexDir: String = args(0)  //"e:\\dbpa\\data\\index\\index-that-works\\Index.wikipediaTraining.Merged."
        val config = new SpotlightConfiguration(args(0));

        val testFileName: String = args(1)  //"e:\\dbpa\\data\\index\\dbpedia36data\\test\\test100k.tsv"
        val output = new PrintWriter(testFileName+".pareval.log")

        //val default : Disambiguator = new DefaultDisambiguator(config)
        //val test : Disambiguator = new GraphCentralityDisambiguator(config)

        val disambiguators = Set(//new TwoStepDisambiguator(config),
                                 new CuttingEdgeDisambiguator(config))

        val paragraphs = AnnotatedTextSource
                            .fromOccurrencesFile(new File(testFileName))

        // Read some text to test.
        disambiguators.foreach( d => evaluate(paragraphs, d, output))

        output.close
    }
}