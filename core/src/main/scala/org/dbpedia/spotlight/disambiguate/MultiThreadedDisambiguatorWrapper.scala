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

package org.dbpedia.spotlight.disambiguate

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

import scala.collection.JavaConverters._
import org.dbpedia.spotlight.log.SpotlightLog
import org.apache.lucene.search.Explanation
import org.dbpedia.spotlight.model._
import scala.actors._
import Actor._
import org.dbpedia.spotlight.exceptions.{DisambiguationException, SearchException, InputException}

/**
 * Default implementation of the disambiguation functionality.
 * Uses classes and parameters we found to perform best.
 * This implementation will change with time, as we evolve the system.
 * If you want a stable implementation, copy this class to MyDisambiguator and use that.
 *
 * @author maxjakob
 * @author pablomendes
 */
class MultiThreadedDisambiguatorWrapper(val disambiguator: Disambiguator) extends Disambiguator  {

    def disambiguate(sfOccurrence: SurfaceFormOccurrence): DBpediaResourceOccurrence = {
        disambiguator.disambiguate(sfOccurrence)
    }

    @throws(classOf[InputException])
    def disambiguate(sfOccurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[DBpediaResourceOccurrence] = {
        val nOccurrences = sfOccurrences.size()

        // Get a reference to this actor
        val caller = self

        // Start an actor to disambiguate one surface form occurrence at a time
        val multiThreadedDisambiguator = actor {
            var i = 0;
            loopWhile( i < nOccurrences) {
                reactWithin(3000) {  //TODO configurable
                    case sfOccurrence: SurfaceFormOccurrence => {
                        //SpotlightLog.info(this.getClass, "Disambiguate: %s", sfOccurrence.surfaceForm)
                        i = i+1
                        // Send the disambiguated occurrence back to the caller
                        try {
                            val disambiguation = disambiguator.disambiguate(sfOccurrence)
                            SpotlightLog.debug(this.getClass, "Sent [%d of %d] %s.", i-1, nOccurrences-1, disambiguation.surfaceForm)
                            caller ! disambiguation
                        } catch {
                            case ex:Throwable =>
                                SpotlightLog.error(this.getClass, "Caught exception trying to disambiguate [%s]: %s", sfOccurrence.surfaceForm, ex)
                                SpotlightLog.debug(this.getClass, "Stack trace: \n%s", ex.getStackTrace.mkString("\n"))
                                caller ! ex
                        }
                    }
                    case TIMEOUT =>
                        caller ! new DisambiguationException("Timed out trying to disambiguate! i="+i)
                        i = i+1
                }
            }
        }

        // Send occurrences for parallel disambiguation
        sfOccurrences.asScala.foreach( o => multiThreadedDisambiguator ! o);

        // Aggregate disambiguated occurrences
        val list = new java.util.ArrayList[DBpediaResourceOccurrence]()
        for ( i <- 0 to nOccurrences-1) {
            receiveWithin(9000) {  // time to wait before each occurrence arrives //TODO configurable
                case disambiguation:DBpediaResourceOccurrence => {
                    SpotlightLog.debug(this.getClass, "Received [%d of %d] ", i, nOccurrences - 1, sfOccurrences.get(i).surfaceForm , disambiguation.surfaceForm)
                    if(disambiguation.context.text.equals(sfOccurrences.get(0).context.text)) { //PATCH by Jo Daiber (temp)
                        //SpotlightLog.trace(this.getClass, "Occurrence came from the same context.")
                        list.add(disambiguation)
                    }
                }
                case e: Throwable =>
                    SpotlightLog.error(this.getClass, "Received Exception %s in result collector. i=%d of %d", e.toString,i.toInt,(nOccurrences-1).toInt)
                case TIMEOUT => {
                    SpotlightLog.error(this.getClass, "Timed out trying to aggregate disambiguations! i=%d of %d", i.toInt,(nOccurrences-1).toInt)
                    //exit()
                }
            }
        }

        return list;
    }

    def bestK(sfOccurrence: SurfaceFormOccurrence, k: Int): java.util.List[DBpediaResourceOccurrence] = {
        disambiguator.bestK(sfOccurrence, k)
    }

    def name() : String = {
        "MultiThreaded:"+disambiguator.name
    }

    def ambiguity(sf : SurfaceForm) : Int = {
        disambiguator.ambiguity(sf)
    }

    def support(resource : DBpediaResource) : Int = {
        disambiguator.support(resource)
    }

    def spotProbability(sfOccurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[SurfaceFormOccurrence] = {
      disambiguator.spotProbability(sfOccurrences)
    }

    @throws(classOf[SearchException])
    def explain(goldStandardOccurrence: DBpediaResourceOccurrence, nExplanations: Int) : java.util.List[Explanation] = {
        disambiguator.explain(goldStandardOccurrence, nExplanations)
    }

    def contextTermsNumber(resource : DBpediaResource) : Int = {
        disambiguator.contextTermsNumber(resource)
    }

    def averageIdf(context : Text) : Double = {
        disambiguator.averageIdf(context)
    }

}
