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
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.disambiguate.Disambiguator
import org.dbpedia.spotlight.exceptions.SearchException

/**
 * Evaluation class. 
 */
class BatchEvaluator(val testSource : Traversable[DBpediaResourceOccurrence], val spotter : Spotter, val disambiguatorSet : Set[Disambiguator])
{
    def listToJavaList[T](l: List[T]) = l.foldLeft(new java.util.ArrayList[T](l.size)){(al, e) => al.add(e); al}

    def spot(current: Text, goldList: List[DBpediaResourceOccurrence]) : List[SurfaceFormOccurrence] = {
        val spottedOcc = spotter.extract(current).toList
        //val spottedOccScala : List[SurfaceFormOccurrence] = JavaConversions.asBuffer(spottedOcc).toList
        //should return a list of SfOcc that are in goldSfSet to save time in disambiguating
        val correctlySpottedOccs : List[SurfaceFormOccurrence] = spottedOcc.filter(spotted => (goldList.find(gold => gold.surfaceForm equals spotted.surfaceForm) != None))

        correctlySpottedOccs
    }

    def disambiguate(disambiguator : Disambiguator, correctlySpottedOccs : List[SurfaceFormOccurrence], goldList : List[DBpediaResourceOccurrence]) : Int = {
        val resOccList = disambiguator.disambiguate(correctlySpottedOccs).toList
        //val resList = JavaConversions.asBuffer(resOccList).toList
        val resMatch = compare(goldList, resOccList)
        resMatch
    }

    def compare(gold : List[DBpediaResourceOccurrence], found : List[DBpediaResourceOccurrence]) : Int =
        {
            var correct = 0
            for (g <- gold)
                {
                    found.find(f => f equals g) match {
                        case Some(correctOcc) => {
                            SpotlightLog.debug(this.getClass, "  Correct: %s -> %s", correctOcc.surfaceForm, correctOcc.resource)
                            correct += 1
                            //print to file
                        }
                        case None => {
                            SpotlightLog.debug(this.getClass, "  WRONG: _REAL_:%s -> %s", g.surfaceForm, g.resource)
                            SpotlightLog.debug(this.getClass, "          _GOT_: %s -> %s", g.surfaceForm, found.map(_.resource).mkString(", "))
                        }
                    }
                }
            correct
        }

    def printStats() {

          // output.print(occId, spotterAcc, disambAcc, ambiguity, trainingSize, disambClass);
    }

    def evaluate()
        {
            var current : String = ""
            var goldList = List[DBpediaResourceOccurrence]()

            var totalOccurrenceCount = 0
            var totalCorrectSfMatches = 0
            var totalCorrectResourceMatches = 0
            var totalUnambiguousCount = 0

            var disambiguationCounters = Map[String,Int]()

            for (testOcc <- testSource)
            {
                SpotlightLog.info(this.getClass, "==%d", totalOccurrenceCount)
                if (!testOcc.context.text.equals(current) && current != "")
                {
                    SpotlightLog.trace(this.getClass, "Processed %d occurrences. Current text: [%s...]", totalOccurrenceCount, current.substring(0,scala.math.min(current.length, 100)))

                    val correctlySpottedOccs = spot(new Text(current), goldList)
                    totalCorrectSfMatches += correctlySpottedOccs.size

                    SpotlightLog.info(this.getClass, "Spotter accuracy: %d/%d = %f", totalCorrectSfMatches, totalOccurrenceCount, totalCorrectSfMatches.toDouble/totalOccurrenceCount.toDouble)

                    //Remove Unambiguous (ambiguity==1)
                    //TODO FIXME Note that we just use the first disambiguator to measure ambiguity. This doesn't guarantee that all disambiguators will find this ambiguous
                    val correctlySpottedAmbOccs = correctlySpottedOccs.filter(occ => disambiguatorSet.head.ambiguity(occ.surfaceForm) > 1)
                    var unamb = correctlySpottedOccs.size - correctlySpottedAmbOccs.size
                    totalUnambiguousCount += unamb;
                    SpotlightLog.info(this.getClass, "Unambiguous: %d/%d = %f", totalUnambiguousCount, totalCorrectSfMatches, totalUnambiguousCount.toDouble/totalCorrectSfMatches.toDouble)

                    if (correctlySpottedAmbOccs.size > 0) {
                        for (disambiguator <- disambiguatorSet)
                        {
                            try {
                                val resMatch = disambiguate(disambiguator, correctlySpottedAmbOccs, goldList)

                                disambiguationCounters = disambiguationCounters.updated(disambiguator.name, disambiguationCounters.get(disambiguator.name).getOrElse(0) + resMatch)

                                SpotlightLog.info(this.getClass, "Disambiguation accuracy %s: %s/%d-%d = %f", disambiguator.name, disambiguationCounters(disambiguator.name), totalCorrectSfMatches, totalUnambiguousCount, disambiguationCounters(disambiguator.name).toDouble/(totalCorrectSfMatches.toDouble-totalUnambiguousCount))
                            }
                            catch {
                                case err : SearchException => SpotlightLog.error(this.getClass, "Disambiguation error in %s; %s", disambiguator.name, err.getMessage)
                            }
                        }
                    } else {
                        SpotlightLog.info(this.getClass, "Nothing to disambiguate. Skipping.")
                    }
                    goldList = List[DBpediaResourceOccurrence]()
                }

                totalOccurrenceCount += 1
                current = testOcc.context.text
                goldList ::= testOcc
            }


        }

}