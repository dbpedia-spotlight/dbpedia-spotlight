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

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.model._
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.util.Profiling._
import org.dbpedia.spotlight.disambiguate._
import org.dbpedia.spotlight.exceptions._
import java.io.{File, PrintStream}

/**
 * Evaluation class. 
 */
class DisambiguationEvaluator(val testSource : Traversable[DBpediaResourceOccurrence], val disambiguatorSet : Set[Disambiguator], val outputFileName : String)
{

    var totalOccurrenceCount = 0
    var totalCorrectResourceMatches = 0

    val outputTopK = 100  // CAUTION cannot be larger than LuceneManager.topResultsLimit

    var disambiguationCounters = Map[String,Int]()
    var unambiguityCounters    = Map[String,Int]()
    var sfNotFoundCounters    = Map[String,Int]()
    var timeCounters          = Map[String,Long]()

    val date = EvalUtils.now()
    val output = new PrintStream(new File(outputFileName));
    val sfNotFoundOutput = new PrintStream(new File(outputFileName+".sfNotFound"))
    val texOutput = new PrintStream(new File(outputFileName+"."+date+".tex"))
    var ambiguousOnly: Boolean = true

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
                        SpotlightLog.info(this.getClass, "  Correct: %s -> %s", correctOcc.surfaceForm, correctOcc.resource)
                        correct += 1
                        //print to file
                    }
                    case None => {
                        SpotlightLog.info(this.getClass, "  WRONG: correct: %s -> %s", g.surfaceForm, g.resource)
                        SpotlightLog.info(this.getClass, "       spotlight: %s -> %s", g.surfaceForm, found.map(_.resource).mkString(", "))
                    }
                }
            }
            correct
        }

    def stats()
    {
        SpotlightLog.info(this.getClass, "Results: %s", outputFileName)
        for (disambiguator <- disambiguatorSet) {
            val total = if (ambiguousOnly) (totalOccurrenceCount.toDouble-unambiguityCounters(disambiguator.name)) else totalOccurrenceCount.toDouble
            val totalText =  if (ambiguousOnly) disambiguationCounters(disambiguator.name)+"/"+totalOccurrenceCount+"-"+unambiguityCounters(disambiguator.name) else disambiguationCounters(disambiguator.name)+"/"+totalOccurrenceCount
            val accuracy = disambiguationCounters(disambiguator.name).toDouble / total
            val msg = "Disambiguation accuracy: "+String.format("%3.2f",double2Double(accuracy*100.0))+"% ("+disambiguator.name+") : "+totalText+" = "+accuracy
            SpotlightLog.info(this.getClass, msg)
            texOutput.print(msg)
        }
        for (disambiguator <- disambiguatorSet) {
            val unambiguity = unambiguityCounters(disambiguator.name).toDouble/(totalOccurrenceCount.toDouble);
            val msg = "Unambiguity: "+String.format("%3.2f",double2Double(unambiguity*100.0))+"% ("+disambiguator.name+ ") : "+unambiguityCounters(disambiguator.name).toDouble +"/"+ totalOccurrenceCount.toDouble
            SpotlightLog.info(this.getClass, msg)
            texOutput.print(msg)
        }
        for (disambiguator <- disambiguatorSet) {
            val notFound = sfNotFoundCounters(disambiguator.name).toDouble/(totalOccurrenceCount.toDouble);
            val msg = "Surface not found: "+String.format("%3.2f",double2Double(notFound*100.0))+"% ("+disambiguator.name+ ") : "+sfNotFoundCounters(disambiguator.name).toDouble +"/"+ totalOccurrenceCount.toDouble
            SpotlightLog.info(this.getClass, msg)
            texOutput.print(msg)
        }
        for (disambiguator <- disambiguatorSet) {
            val avgTime = timeCounters(disambiguator.name).toDouble/(totalOccurrenceCount.toDouble);
            val msg = "Avg Disamb. Time: "+ formatTime(avgTime.toLong)+" ("+disambiguator.name+ ") : "+timeCounters(disambiguator.name).toDouble +"/"+ totalOccurrenceCount.toDouble
            SpotlightLog.info(this.getClass, msg)
            texOutput.print(msg)
        }
    }

    def init() {
        for (disambiguator <- disambiguatorSet) {
            disambiguationCounters = disambiguationCounters.updated(disambiguator.name, 0)
            unambiguityCounters = unambiguityCounters.updated(disambiguator.name, 0)
            sfNotFoundCounters = sfNotFoundCounters.updated(disambiguator.name, 0)
            timeCounters = timeCounters.updated(disambiguator.name, 0)
        }
    }

    def evaluate(maxNumberOfOutputResults: Int = Integer.MAX_VALUE, //how many n-best disambiguations to write to log
                 filter: DBpediaResource => DBpediaResource // decide if keeps resource (potentially transform resource to something)
                         = { r => r } ) // by default return itself
    {
        // set all counters to 0
        init();

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

        for (testOcc <- testSource)
        {
            totalOccurrenceCount += 1
            if(totalOccurrenceCount%10 == 0) {
                SpotlightLog.info(this.getClass, "==%d", totalOccurrenceCount)
            }
            //SpotlightLog.trace(this.getClass, "Processed %d occurrences. Current text: [%s...]", totalOccurrenceCount, correctOccurrence.context.text.substring(0,scala.math.min(current.length, 100)))

            for (disambiguator <- disambiguatorSet)
            {

                val occId = if (testOcc.id.isEmpty) totalOccurrenceCount.toString else testOcc.id

                // stats about the test occurrence
                val ambiguity = disambiguator.ambiguity(testOcc.surfaceForm)
                val trainingVectorLength = "NA" //disambiguator.contextTermsNumber(sptlResultOcc.resource)  //TODO bring this back when TermVectors are stored in the CONTEXT field
                val correctUriSupport = disambiguator.support(testOcc.resource) // testOcc.resource.support
                val queryWordTypes = testOcc.context.text.split("\\W+").toSet.size // simple tokenization and counting of terms
                val averageIdf = "NA"  // disambiguator.averageIdf(correctOccurrence.context)


                val spotProb : java.lang.Double = disambiguator match {
                    //case d: MergedOccurrencesDisambiguator => d.spotProbability(correctOccurrence.surfaceForm);
                    case _ => 1.0; //TODO implement for other disambiguators
                }
                //SpotlightLog.info(this.getClass, "Spot probability for "%s=%s", correctOccurence.surfaceForm, spotProb)

                var unambiguous = 0
                var sfNotFound = 0
                var disambAccuracy = 0
                if (ambiguity>=1) {
                    try
                    {
                        SpotlightLog.debug(this.getClass, disambiguator.name)
                        SpotlightLog.debug(this.getClass, "Ambiguity for %s=%d", testOcc.surfaceForm, ambiguity)

                        //val sfOccWrapped = List(new SurfaceFormOccurrence(correctOccurrence.surfaceForm, correctOccurrence.context, correctOccurrence.textOffset, correctOccurrence.provenance))
                        //val resList = disambiguate(disambiguator, sfOccWrapped)

                        val sfOcc = new SurfaceFormOccurrence(testOcc.surfaceForm, testOcc.context, testOcc.textOffset, testOcc.provenance)

                        val bestK = timed(storeTime(disambiguator)) {
                            disambiguator.bestK(sfOcc,outputTopK) //ATTENTION!!! the order of occurrences returned here will be used to assess if this was a correct or incorrect disambiguation
                        }


                        //DEBUG
                        //val watch = bestK.map(o => "%s \t %.5f \t %.5f \t %.5f" .format(o.resource.uri, o.resource.prior, o.contextualScore, o.similarityScore) )

                        if (ambiguity==1) {
                            unambiguous = 1
                            if (ambiguousOnly)
                                SpotlightLog.debug(this.getClass, "Nothing to disambiguate. Unambiguous occurrence. Skipping.")
                        }


                        val spotlightDecision = if (bestK.size<=0) new DBpediaResource("NIL") else filter(bestK.head.resource)

                        if(testOcc.resource equals spotlightDecision) {
                            if ( (ambiguity>1) || !ambiguousOnly ) { // count only if ambiguity is higher than one or it's not ambiguous only
                                disambiguationCounters = disambiguationCounters.updated(disambiguator.name, disambiguationCounters.get(disambiguator.name).getOrElse(0) + 1)
                                SpotlightLog.debug(this.getClass, "  **     correct: %.5f \t %.5f \t %.5f \t %s", bestK.head.resource.prior, bestK.head.contextualScore, bestK.head.similarityScore, spotlightDecision.uri+" / "+bestK.head.resource.uri)
                                disambAccuracy = 1
                            }
                        }

                        //val sortedOccs = bestK.sortBy( o => o.resource.prior ) // Change order here for training data.
                        var i = 0
                        for(sptlResultOcc <- bestK) {
                            val filtered = filter(sptlResultOcc.resource)
                            if(i>0 && testOcc.resource.equals(sptlResultOcc.resource)) {
                                SpotlightLog.debug(this.getClass, "  **     correct: %.5f \t %.5f \t %.5f \t %s", sptlResultOcc.resource.prior, sptlResultOcc.contextualScore, sptlResultOcc.similarityScore, sptlResultOcc.resource)
                            }
                            else {
                                //incorrectScores ::= score.toDouble
                                //SpotlightLog.debug(this.getClass, "  WRONG: correct: %s -> %s", correctOccurrence.surfaceForm, correctOccurrence.resource)
                                SpotlightLog.debug(this.getClass, "       spotlight: %.5f \t %.5f \t %.5f \t %s", sptlResultOcc.resource.prior, sptlResultOcc.contextualScore, sptlResultOcc.similarityScore, filtered.uri+" / "+sptlResultOcc.resource)
                                //println(disambiguator.explain(correctOccurrence, 100))
                            }

                            // write stats for this disambiguator
                            if (i < maxNumberOfOutputResults) {
                                output.append(occId+"\t"+
                                          disambAccuracy+"\t"+
                                          testOcc.surfaceForm.name+"\t"+
                                          testOcc.resource.uri+"\t"+
                                          filtered.uri+"/"+sptlResultOcc.resource.uri+"\t"+
                                          disambiguator.name+"\t"+
                                          correctUriSupport+"\t"+
                                          sptlResultOcc.resource.prior +"\t"+
                                          sptlResultOcc.similarityScore.toString+"\t"+
                                          sptlResultOcc.percentageOfSecondRank.toString+"\t"+
                                          ambiguity+"\t"+
                                          sptlResultOcc.contextualScore+"\t"+
                                          trainingVectorLength+"\t"+
                                          queryWordTypes+"\t"+
                                          averageIdf+"\n"
                                )
                            }
                            i = i + 1

                            //givenAnswers = sortedOccs.map(annotatedResOcc => annotatedResOcc.resource.uri+"("+annotatedResOcc.similarityScore.toString+")").mkString("")
                        }
                        if (disambAccuracy==0)
                            if ( !ambiguousOnly || (ambiguity>1))
                                SpotlightLog.debug(this.getClass, "  **   not found: %.5s \t %.5s \t %.5s \t %s", "NA", "NA", "NA", testOcc.resource)

                    }
                    catch
                    {
                        case err : SearchException => SpotlightLog.error(this.getClass, "Disambiguation error in %s; %s", disambiguator.name, err.getMessage)
                        case err : InputException => SpotlightLog.error(this.getClass, "Disambiguation error in %s; %s", disambiguator.name, err.getMessage)
                        case e: ItemNotFoundException =>  {
                            if (!ambiguousOnly && testOcc.resource.uri=="NIL") {
                                disambAccuracy = 1
                                disambiguationCounters = disambiguationCounters.updated(disambiguator.name, disambiguationCounters.get(disambiguator.name).getOrElse(0) + 1)
                                SpotlightLog.debug(this.getClass, "  **     correct: predicted NIL by 'surface form + context not found'.")
                            } else {
                                SpotlightLog.debug(this.getClass, "  **   not found: Surface Form is in index but context does not match. (%s). ", testOcc.surfaceForm)
                            }
                        }
                    }
                } else {
                    if (ambiguity==0) {  // ambiguity of zero means that the surface form was not found
                        sfNotFound = 1
                        sfNotFoundOutput.println(testOcc.surfaceForm)
                        if (!ambiguousOnly && testOcc.resource.uri=="NIL") {
                            disambAccuracy = 1
                            disambiguationCounters = disambiguationCounters.updated(disambiguator.name, disambiguationCounters.get(disambiguator.name).getOrElse(0) + 1)
                            SpotlightLog.debug(this.getClass, "  **     correct: predicted NIL by 'surface form not found'.")
                        } else {
                            SpotlightLog.debug(this.getClass, "  **   not found: Surface Form not in index (%s). ", testOcc.surfaceForm)
                        }
                    }
                    // write stats for this disambiguator
                    output.append(occId+"\t"+
                        disambAccuracy+"\t"+
                        testOcc.surfaceForm.name+"\t"+
                        testOcc.resource.uri+"\t"+
                        "NIL\t"+  //sptlResultOcc.resource.uri+"\t"+
                        disambiguator.name+"\t"+
                        correctUriSupport+"\t"+
                        "NA\t"+   //sptlResultOcc.resource.prior +"\t"+
                        "NA\t"+   //sptlResultOcc.similarityScore.toString+"\t"+
                        "NA\t"+   //sptlResultOcc.percentageOfSecondRank.toString+"\t"+
                        ambiguity+"\t"+
                        "NA\t"+   //sptlResultOcc.contextualScore+"\t"+
                        trainingVectorLength+"\t"+
                        queryWordTypes+"\t"+
                        averageIdf+"\n"
                    )

                }

                unambiguityCounters = unambiguityCounters.updated(disambiguator.name, unambiguityCounters.get(disambiguator.name).getOrElse(0) + unambiguous)
                sfNotFoundCounters = sfNotFoundCounters.updated(disambiguator.name, sfNotFoundCounters.get(disambiguator.name).getOrElse(0) + sfNotFound)

            }  // foreach disambiguator

            // update logger only each 100 occurrences.
            if (totalOccurrenceCount % 100 == 0) stats();

        } // foreach occurrence

        SpotlightLog.info(this.getClass, "===== TOTAL RESULTS:")
        stats()

        output.close();
        sfNotFoundOutput.close();
        texOutput.close();
    }

    def storeTime(disambiguator: Disambiguator) = (delta:Long) => {
        timeCounters = timeCounters.updated(disambiguator.name, timeCounters.get(disambiguator.name).getOrElse(0.toLong) + delta)
        //println(disambiguator.name + "  " + formatTime(delta))
    }
}