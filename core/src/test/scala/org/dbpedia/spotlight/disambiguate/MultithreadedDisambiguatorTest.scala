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

import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, SurfaceFormOccurrence}
import actors._
import actors.Actor._
import org.junit.Test
import scalaj.collection.Imports._

/**
 * TODO build real test with disambiguators
 * @author pablomendes
 */

class MultithreadedDisambiguatorTest {

    @Test
    def test() {
        val list = new java.util.ArrayList[Int]()
        1 to 3 foreach (n => list.add(n))
        disambiguate(list)
    }

    def disambiguate(sfOccurrences: java.util.List[Int]) {

        val nOccurrences = sfOccurrences.size()

        val caller = self

        val multiThreadedDisambiguator = actor {
            for ( i <- 1 to nOccurrences) {
                receiveWithin(1000) {
                    case (caller: Actor, sfOccurrence: Int) =>
                        println("multiThreadedDisambiguator called")
                        caller ! "disambiguator.disambiguate(sfOccurrence)"
                    case TIMEOUT => println(" Timed out trying to disambiguate! ")
                }
            }
        }

        sfOccurrences.asScala.foreach( o => multiThreadedDisambiguator ! (caller, o))

        for ( i <- 1 to nOccurrences) {
            receiveWithin(3000) {
                case occurrence: String => println("list.add(occurrence)")
                case TIMEOUT => println(" Timed out trying to aggregate disambiguations! ")
            }
        }

        //Thread.sleep(5000)

    }
}