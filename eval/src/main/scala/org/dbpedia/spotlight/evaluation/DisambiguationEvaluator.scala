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
import org.dbpedia.spotlight.model._
import scala.collection.JavaConversions._
import java.io.PrintStream

import org.dbpedia.spotlight.evaluation.Profiling._
import org.dbpedia.spotlight.lucene.disambiguate.MergedOccurrencesDisambiguator
import org.dbpedia.spotlight.disambiguate._
import org.dbpedia.spotlight.exceptions._

/**
 * Evaluation class. 
 */
class DisambiguationEvaluator(val testSource : Traversable[DBpediaResourceOccurrence], val disambiguatorSet : Set[Disambiguator], val output : PrintStream)
{
    // moved to BaseSearcher.getNumberOfOccurrences
    //val PRIOR_DENOMINATOR : Double = 69772256.0

    private val LOG = LogFactory.getLog(this.getClass)
    var totalOccurrenceCount = 0
    var totalCorrectResourceMatches = 0

    val outputTopK = 100  // CAUTION cannot be larger than LuceneManager.topResultsLimit

    var disambiguationCounters = Map[String,Int]()
    var unambiguityCounters    = Map[String,Int]()
    var sfNotFoundCounters    = Map[String,Int]()
    var timeCounters          = Map[String,Long]()

    def listToJavaList[T](l: List[T]) = l.foldLeft(new java.util.ArrayList[T](l.size)){(al, e) => al.add(e); al}

    def disambiguate(disambiguator : Disambiguator, correctlySpottedOccs : List[SurfaceFormOccurrence], goldList : List[DBpediaResourceOccurrence]) : Int = {
        val resOccList = disambiguator.disambiguate(listToJavaList(correctlySpottedOccs))
        val resList = resOccList.toList
        val resMatch = compare(goldList, resList)
        resMatch
    }

    def disambiguate(disambiguator : Disambiguator, correctlySpottedOccs : List[SurfaceFormOccurrence]) : List[DBpediaResourceOccurrence] = {
        val resOccList = disambiguator.disambiguate(listToJavaList(correctlySpottedOccs))
        resOccList.toList
    }

//    def bestK(disambiguator : Disambiguator, correctlySpottedOcc : SurfaceFormOccurrence) : List[DBpediaResourceOccurrence] = {
//        disambiguator.bestK(correctlySpottedOcc, outputTopK).toList.sortBy(_.similarityScore).reverse //FIXME shouldn't the sorting be done inside? think of disambiguators that don' t sort by score (e.g. prior disambiguator)
//    }

    def compare(gold : List[DBpediaResourceOccurrence], found : List[DBpediaResourceOccurrence]) : Int =
        {
            var correct = 0
            for (g <- gold)
            {
                found.find(f => f equals g) match {
                    case Some(correctOcc) => {
                        LOG.info("  Correct: "+correctOcc.surfaceForm + " -> " + correctOcc.resource)
                        correct += 1
                        //print to file
                    }
                    case None => {
                        LOG.info("  WRONG: correct: "+g.surfaceForm+" -> "+g.resource);
                        LOG.info("       spotlight: "+g.surfaceForm+" -> "+found.map(_.resource).mkString(", "));
                    }
                }
            }
            correct
        }

    def stats()
    {
        for (disambiguator <- disambiguatorSet) {
            val accuracy = disambiguationCounters(disambiguator.name).toDouble/(totalOccurrenceCount.toDouble-unambiguityCounters(disambiguator.name))
            LOG.info("Disambiguation accuracy: "+String.format("%3.2f",double2Double(accuracy*100.0))+"% ("+disambiguator.name+") : "+disambiguationCounters(disambiguator.name)+"/"+totalOccurrenceCount+"-"+unambiguityCounters(disambiguator.name)+" = "+accuracy)
        }
        for (disambiguator <- disambiguatorSet) {
            val unambiguity = unambiguityCounters(disambiguator.name).toDouble/(totalOccurrenceCount.toDouble);
           LOG.info("Unambiguity: "+String.format("%3.2f",double2Double(unambiguity*100.0))+"% ("+disambiguator.name+ ") : "+unambiguityCounters(disambiguator.name).toDouble +"/"+ totalOccurrenceCount.toDouble);
        }
        for (disambiguator <- disambiguatorSet) {
            val notFound = sfNotFoundCounters(disambiguator.name).toDouble/(totalOccurrenceCount.toDouble);
           LOG.info("Surface not found: "+String.format("%3.2f",double2Double(notFound*100.0))+"% ("+disambiguator.name+ ") : "+sfNotFoundCounters(disambiguator.name).toDouble +"/"+ totalOccurrenceCount.toDouble);
        }
        for (disambiguator <- disambiguatorSet) {
            val avgTime = timeCounters(disambiguator.name).toDouble/(totalOccurrenceCount.toDouble);
            LOG.info("Avg Disamb. Time: "+ formatTime(avgTime.toLong)+" ("+disambiguator.name+ ") : "+timeCounters(disambiguator.name).toDouble +"/"+ totalOccurrenceCount.toDouble);
        }
    }

    def evaluate()
    {
        //header
        output.append("occId\t"+                // occurrence ID
                      "disambAccuracy\t"+       // correct: 1, incorrect: 0
                      "surfaceForm\t"+          // surface form in question
                      "correctURI\t"+           // correct resource of this occurrence   TODO print type?
                      "spotlightURI\t"+         // resource given as result by Spotlight
                      "disambiguator\t"+        // type of disambiguator
                      "support\t"+      // number of Wikipedia inlinks for this resource
                      "prior\t"+                // prior probability of seeing the spotlightURI; normalized uriCount
                      "score\t"+                // final similarity score (mixtures may change this from the contextual)
                      "percentageOfSecond\t"+   // context similarity score of second ranked divided by context similarity score of first ranked
                      "ambiguity\t"+            // number of URIs that the surface form can refer to
                      "contextualScore\t"+             // probability of this being a relevant surface form
                      "trainingVectorLength\t"+ // terms in the context field
                      "queryWordTypes\t"+       // word types in the query
                      "averageIdf\n"            // average idf of the query terms
                    //"spotProb\t"+             // probability of this being a relevant surface form
        )

        for (testOcc <- testSource) //TODO it sounds like ultimately we'd need to get paragraphs with occurrences instead. Think if graph disamb
        {
            totalOccurrenceCount += 1
            if(totalOccurrenceCount%10 == 0) {
                LOG.info("=="+totalOccurrenceCount)
            }
            //LOG.trace("Processed "+totalOccurrenceCount+" occurrences. Current text: ["+testOcc.context.text.substring(0,scala.math.min(current.length, 100))+"...]")

            for (disambiguator <- disambiguatorSet)
            {

                val occId = if (testOcc.id.isEmpty) totalOccurrenceCount.toString else testOcc.id

                val ambiguity = disambiguator.ambiguity(testOcc.surfaceForm)

                val spotProb : java.lang.Double = disambiguator match {
                    //case d: MergedOccurrencesDisambiguator => d.spotProbability(testOcc.surfaceForm);
                    case _ => 1.0; //TODO implement for other disambiguators
                }
                //LOG.info("Spot probability for "+testOcc.surfaceForm+"="+spotProb)

                var unambiguous = 0
                var sfNotFound = 0

                if (ambiguity > 1)
                {
                    try
                    {
                        LOG.debug(disambiguator.name);
                        LOG.debug("Ambiguity for "+testOcc.surfaceForm+"="+ambiguity)

                        //val sfOccWrapped = List(new SurfaceFormOccurrence(testOcc.surfaceForm, testOcc.context, testOcc.textOffset, testOcc.provenance))
                        //val resList = disambiguate(disambiguator, sfOccWrapped)

                        val sfOcc = new SurfaceFormOccurrence(testOcc.surfaceForm, testOcc.context, testOcc.textOffset, testOcc.provenance)

                        val bestK = timed(storeTime(disambiguator)) {
                            disambiguator.bestK(sfOcc,outputTopK) //ATTENTION!!! the order of occurrences returned here will be used to assess if this was a correct or incorrect disambiguation
                        }

                        val watch = bestK.map(o => "%s \t %.5f \t %.5f \t %.5f" .format(o.resource.uri, o.resource.prior, o.contextualScore, o.similarityScore) )

                        //val sptlDecision = sortedOccs.head
                        //score = sptlDecision.similarityScore.toString
                        //precentageOfSecond = sptlDecision.percentageOfSecondRank.toString
                        //sptlResultURI = sptlDecision.resource.uri
                        if(testOcc.resource equals bestK.head.resource) {
                            disambiguationCounters = disambiguationCounters.updated(disambiguator.name, disambiguationCounters.get(disambiguator.name).getOrElse(0) + 1)
                        }

                        // simple tokenization and counting of terms
                        val queryWordTypes = testOcc.context.text.split("\\W+").toSet.size

                        val averageIdf = "NA"  // disambiguator.averageIdf(testOcc.context)

                        val sortedOccs = bestK.sortBy( o => o.resource.prior ) // Change order here for training data.

                        for(sptlResultOcc <- bestK) {

                            var disambAccuracy = 0
                            if(testOcc.resource equals sptlResultOcc.resource) {
                                disambAccuracy = 1
                                //correctScores ::= score.toDouble
                                //LOG.debug(sptlResultOcc.surfaceForm);
                                LOG.debug("  **     correct: %.5f \t %.5f \t %.5f \t %s".format(sptlResultOcc.resource.prior, sptlResultOcc.contextualScore, sptlResultOcc.similarityScore, sptlResultOcc.resource))
                            }
                            else {
                                //incorrectScores ::= score.toDouble
                                //LOG.debug("  WRONG: correct: " + testOcc.surfaceForm + " -> " + testOcc.resource);
                                LOG.debug("       spotlight: %.5f \t %.5f \t %.5f \t %s".format(sptlResultOcc.resource.prior, sptlResultOcc.contextualScore, sptlResultOcc.similarityScore, sptlResultOcc.resource))
                                //println(disambiguator.explain(testOcc, 100))
                            }


                            val trainingSetSize = testOcc.resource.support //disambiguator.support(sptlResultOcc.resource)
                            //val prior = (testOcc.resource.support / 69772256)
                            val trainingVectorLength = "NA" //disambiguator.contextTermsNumber(sptlResultOcc.resource)  //TODO bring this back when TermVectors are stored in the CONTEXT field

                            // write stats for this disambiguator
                            output.append(occId+"\t"+
                                          disambAccuracy+"\t"+
                                          testOcc.surfaceForm.name+"\t"+
                                          testOcc.resource.uri+"\t"+
                                          sptlResultOcc.resource.uri+"\t"+
                                          disambiguator.name+"\t"+
                                          trainingSetSize+"\t"+
                                          sptlResultOcc.resource.prior +"\t"+
                                          sptlResultOcc.similarityScore.toString+"\t"+
                                          sptlResultOcc.percentageOfSecondRank.toString+"\t"+
                                          ambiguity+"\t"+
                                          sptlResultOcc.contextualScore+"\t"+
                                          trainingVectorLength+"\t"+
                                          queryWordTypes+"\t"+
                                          averageIdf+"\n"
                            )

                            //givenAnswers = sortedOccs.map(annotatedResOcc => annotatedResOcc.resource.uri+"("+annotatedResOcc.similarityScore.toString+")").mkString("")
                        }
                    }
                    catch
                    {
                        case err : SearchException => LOG.error("Disambiguation error in "+disambiguator.name+ "; " + err.getMessage)
                        case err : InputException => LOG.error("Disambiguation error in "+disambiguator.name+ "; " + err.getMessage)
                    }
                } else {
                    if (ambiguity==0) {  // ambiguity of zero means that the surface form was not found
                        sfNotFound = 1
                        LOG.debug("Nothing to disambiguate. Surface Form not found ("+testOcc.surfaceForm+"). Skipping.");
                    }
                    if (ambiguity==1) {
                        unambiguous = 1
                        LOG.debug("Nothing to disambiguate. Unambiguous occurrence. Skipping.");
                    }
                }

                unambiguityCounters = unambiguityCounters.updated(disambiguator.name, unambiguityCounters.get(disambiguator.name).getOrElse(0) + unambiguous)
                sfNotFoundCounters = sfNotFoundCounters.updated(disambiguator.name, sfNotFoundCounters.get(disambiguator.name).getOrElse(0) + sfNotFound)


            }

            // update logger only each 100 occurrences.
            if (totalOccurrenceCount % 100 == 0) stats();
        }

        LOG.info("===== TOTAL RESULTS:");
        stats()
    }

    def storeTime(disambiguator: Disambiguator) = (delta:Long) => {
        timeCounters = timeCounters.updated(disambiguator.name, timeCounters.get(disambiguator.name).getOrElse(0.toLong) + delta)
        //println(disambiguator.name + "  " + formatTime(delta))
    }
}