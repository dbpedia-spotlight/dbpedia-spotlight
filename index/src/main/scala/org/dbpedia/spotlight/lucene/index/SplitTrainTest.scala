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

package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import java.io.{PrintStream, File}
import util.Random
import org.dbpedia.spotlight.io.{OccurrenceSource, FileOccurrenceSource}

/**
 * User: Max
 * Date: 18.08.2010
 * Time: 15:06:08
 * Make (clean) training and testing files of ambiguous occurrences.
 */

object SplitTrainTest
{
    def main(args : Array[String]) {
        //val trainingPercentage = 0.95
        //val baseDir = "e:/data/split.train-"+(trainingPercentage*100).toInt+".amb/"
        val baseDir = args(0)
        val trainingPercentage = args(1).toDouble

        SpotlightLog.info(this.getClass, "Splitting ambiguous and unambiguous occurrences. Training samples in percent: %f", trainingPercentage)
        SpotlightLog.warn(this.getClass, "**********************************************************************")
        SpotlightLog.warn(this.getClass, "WARNING: this assumes that the occurrences are sorted by surface form!")
        SpotlightLog.warn(this.getClass, "**********************************************************************")

        // tsv file already has types and was filtered
        val sfSortedOccurrencesFileName = "data/WikipediaOccurrences-IDs-clean_enwiki-20100312.sfSorted.tsv"
        val occurrenceSource = FileOccurrenceSource.fromFile(new File(sfSortedOccurrencesFileName))

        // assign types and filter occurrences
//        val wikipediaFileName = "c:\\wikipediaDump\\en\\20100312\\enwiki-20100312-pages-articles.sfSorted.xml"
//        val wikiSource = WikiOccurrenceSource.fromXMLDumpFile(new File(wikipediaFileName))
//        val typeDictFileName = "data/dbpedia/instance_types_en.tsv"
//        val typedOccSource = new TypeAdder(wikiSource, new File(typeDictFileName))
//        val occurrenceSource = IndexConfiguration.occurrenceFilter.filter(typedOccSource)

        val trainingStream = new PrintStream(baseDir+"training.sfSorted.tsv", "UTF-8")
        val testingStream  = new PrintStream(baseDir+"testing.tsv", "UTF-8")

        split(occurrenceSource, trainingPercentage, trainingStream, testingStream)
    }

    def split(occurrenceSource : OccurrenceSource, trainingPercentage : Double, trainingStream : PrintStream, testingStream : PrintStream){
        var currentSurfaceFormString = ""
        var occsForCurrentSurfaceForm = List[DBpediaResourceOccurrence]()

        var counter = 0

        for (occ <- occurrenceSource) {
            if (!(occ.surfaceForm.name equals currentSurfaceFormString) && !(currentSurfaceFormString equals "")) {
                write(occsForCurrentSurfaceForm, trainingPercentage, trainingStream, testingStream)
                occsForCurrentSurfaceForm = List[DBpediaResourceOccurrence]()
            }
            currentSurfaceFormString = occ.surfaceForm.name
            occsForCurrentSurfaceForm ::= occ

            counter += 1
            if (counter % 100000 == 0)
                SpotlightLog.info(this.getClass, "processed %d occurrences", counter)
        }
        write(occsForCurrentSurfaceForm, trainingPercentage, trainingStream, testingStream)

        SpotlightLog.info(this.getClass, "Done. Processed %d occurrences", counter)
        trainingStream.close
        testingStream.close
    }


    private def splitUriPure(l : List[DBpediaResourceOccurrence]) : List[List[DBpediaResourceOccurrence]] = {
        var allOccs = List[List[DBpediaResourceOccurrence]]()
        var occsForUri = List[DBpediaResourceOccurrence]()
        var prevUri = ""
        for (occ <- l.sortBy(_.resource.uri)) {
            if (!(occ.resource.uri equals prevUri) && !(prevUri equals "")) {
                allOccs ::= Random.shuffle(occsForUri)
                occsForUri = List[DBpediaResourceOccurrence]()
            }
            occsForUri ::= occ
            prevUri = occ.resource.uri
        }
        allOccs ::= Random.shuffle(occsForUri)
        Random.shuffle(allOccs)
    }

    private def write(occsForSurfaceForm : List[DBpediaResourceOccurrence],
                               trainingPercentage : Double,
                               trainingStream : PrintStream,
                               testingStream : PrintStream) {

        // sort by URIs; put occurrences of the same URI in a list:  List( ListForURI1, ListForURI2, ...)
        val uriPureOccsLists = splitUriPure(occsForSurfaceForm)

        // only one URI for this surface form?
        val unambiguous = uriPureOccsLists.length <= 1

        for (occListForUri <- uriPureOccsLists) {
            // number of training samples
            var trainingCountdown = (occListForUri.length*trainingPercentage).round

            for (occ <- occListForUri) {
                if (unambiguous || (trainingCountdown > 0)) {
                    trainingStream.println(occ.toTsvString)
                    trainingCountdown -= 1
                }
                else {
                    testingStream.println(occ.toTsvString)
                }
            }
        }
    }

}