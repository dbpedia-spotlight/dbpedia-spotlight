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

package org.dbpedia.spotlight.util

import org.dbpedia.spotlight.log.SpotlightLog
import scala.collection.JavaConversions._
import java.io.File
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.disambiguate.{DefaultDisambiguator, Disambiguator}
import org.dbpedia.spotlight.string.ParseSurfaceFormText
import org.dbpedia.spotlight.sparql.SparqlQueryExecuter
import org.dbpedia.spotlight.exceptions.InputException
import java.net.URLEncoder

@deprecated("please use org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters")
class AnnotationFilter(val config: SpotlightConfiguration)
{

    // List of similarity scores from a development test run. Used to map confidence onto similarity score thresholds
    //val simThresholdList = List(0.875155268207,1.13201455044,1.23704027675,1.31043336244,1.37249636953,1.42678636431,1.47426818611,1.51699219814,1.5589122902,1.59833069873,1.63325972393,1.66743306586,1.7028011287,1.73513348674,1.7694202019,1.80213153211,1.83303477587,1.86295521865,1.89424885596,1.92353352553,1.95351725373,1.9834741473,2.01226448667,2.04107695223,2.0709070281,2.09985587688,2.12969398628,2.1595095078,2.18906425694,2.21847120257,2.24710029526,2.27635654737,2.30462947212,2.33447480288,2.36373952642,2.39334667746,2.4242092477,2.45480585618,2.48515738333,2.51675826106,2.5474261013,2.5797234845,2.61152222498,2.64228961515,2.67497596637,2.70977720029,2.74521420569,2.77768978363,2.81286198864,2.8489015388,2.88619747049,2.92265128786,2.95716825934,2.99291319769,3.03089856712,3.07175352309,3.11190546662,3.15373792839,3.19605117701,3.24065431276,3.28548179175,3.33270072677,3.38246673291,3.42992931096,3.47811829546,3.52977257069,3.58448599601,3.63959105997,3.69651854579,3.75468738153,3.81389593342,3.87974540897,3.94348987045,4.01026528803,4.08429597467,4.16200482218,4.24415476482,4.33359968944,4.4205907434,4.51784007216,4.61750527346,4.71220335034,4.82202004867,4.94565828569,5.07801206921,5.218870047,5.37478448951,5.54666888346,5.72957153978,5.91928811601,6.14764451981,6.40239167862,6.70676075179,7.06314127501,7.48235848422,7.91990547042,8.36265466001,8.83694069442,9.39367907709,10.4033660975)
    val simThresholdList = config.getSimilarityThresholds.map(_.doubleValue)

    // Responsible for sending SPARQL queries to the endpoint (results will be used for filtering)
    val sparqlExecuter = new SparqlQueryExecuter(config.getSparqlMainGraph(),config.getSparqlEndpoint());

    object ListColor extends Enumeration {
        type ListColor = Value
        val Blacklist, Whitelist = Value
    }
    import ListColor._


    def filter(occs : java.util.List[DBpediaResourceOccurrence],
               confidence : Double,
               targetSupport : Int,
               dbpediaTypes : java.util.List[OntologyType],
               sparqlQuery : String,
               blacklist : Boolean,
               coreferenceResolution : Boolean) : java.util.List[DBpediaResourceOccurrence] = {

        val filteredOccs = filter(occs.toList, confidence, targetSupport, dbpediaTypes, sparqlQuery, blacklist, coreferenceResolution)
        filteredOccs
    }

    def filter(occs : List[DBpediaResourceOccurrence],
               confidence : Double,
               targetSupport : Int,
               dbpediaTypes : java.util.List[OntologyType],
               sparqlQuery : String,
               blacklist : Boolean,
               coreferenceResolution : Boolean): List[DBpediaResourceOccurrence] = {

        if (0 > confidence || confidence > 1) {
            throw new InputException("confidence must be between 0 and 1; got "+confidence)
        }

        var filteredOccs = occs

        val listColor = if(blacklist) Blacklist else Whitelist

        if (coreferenceResolution) filteredOccs = buildCoreferents(filteredOccs)
        filteredOccs = filterBySupport(filteredOccs, targetSupport)
        filteredOccs = filterByConfidence(filteredOccs, confidence)
        filteredOccs = filterByType(filteredOccs, if(dbpediaTypes == null) List() else dbpediaTypes.toList, listColor)
        filteredOccs = filterBySupport(filteredOccs, targetSupport)

        if(sparqlQuery != null && sparqlQuery != "") {
            filteredOccs = filterBySparql(filteredOccs, sparqlQuery, listColor, sparqlExecuter)
        }

        filteredOccs = filteredOccs.sortBy(_.textOffset)    // sort by offset (because we observed returning unsorted lists in some cases)
        filteredOccs
    }

    private def isCoreferent(previous : SurfaceForm, later : SurfaceForm) : Boolean = {
        val prevSFWords = previous.name.split(" ")
        val laterSFWords = later.name.split(" ")
        ( (laterSFWords.length == 1 &&
                prevSFWords.filterNot(word => word.substring(0,1) equals word.substring(0,1).toUpperCase).isEmpty &&
                prevSFWords.contains(laterSFWords.head))
                //|| (prevSFWords.last equals laterSFWords.last)
                )
    }

    private def buildCoreferents(occs : List[DBpediaResourceOccurrence]) : List[DBpediaResourceOccurrence] = {
        // this is a heuristic and has nothing to do with proper coreference resolution!!!
        var backwardIdx = occs.length
        occs.reverse.map(laterOcc => {
            backwardIdx -= 1
            val coreferentOcc = occs.slice(0, backwardIdx).find(prevOcc => {
                val coreferring = isCoreferent(prevOcc.surfaceForm, laterOcc.surfaceForm)
                if (coreferring)
                    SpotlightLog.info(this.getClass, "found coreferent: %s at position %d probably coreferring to %s at position %d; copying %s", laterOcc.surfaceForm, laterOcc.textOffset, prevOcc.surfaceForm, prevOcc.textOffset, prevOcc.resource)
                coreferring
            })
            if (coreferentOcc != None) {
                new DBpediaResourceOccurrence(laterOcc.id,
                    coreferentOcc.get.resource,
                    laterOcc.surfaceForm,
                    laterOcc.context,
                    laterOcc.textOffset,
                    laterOcc.provenance,
                    coreferentOcc.get.similarityScore,           // what to put here?
                    coreferentOcc.get.percentageOfSecondRank)    // what to put here?
            }
            else {
                laterOcc
            }
        }).reverse

        //        occs.reverse.filterNot(laterOcc => {
        //            val laterSFWords = laterOcc.surfaceForm.name.split(" ")
        //            backwardIdx -= 1
        //            occs.slice(0, backwardIdx).find(prevOcc => {
        //                val prevSFWords = prevOcc.surfaceForm.name.split(" ")
        //                val isCoreferent = ( (laterSFWords.length == 1 && prevSFWords.contains(laterSFWords.head)) ||
        //                                     (prevSFWords.last equals laterSFWords.last) )
        //                if (isCoreferent)
        //                    SpotlightLog.info(this.getClass, "filtered out as coreferent: %s at position %d probably coreferring to %s at position %d", laterOcc.surfaceForm, laterOcc.textOffset, prevOcc.surfaceForm, prevOcc.textOffset)
        //                isCoreferent
        //            }) != None
        //        }).reverse
    }


    private def ersaseFalsyResources(occs : List[DBpediaResourceOccurrence]) : List[DBpediaResourceOccurrence] = {
        // there are still lists in the index for some reason.
        occs.filterNot(_.resource.uri.startsWith("List_of_"))
    }

    // filter by confidence threshold
    def filterByConfidence(occs : List[DBpediaResourceOccurrence], confidence : Double) : List[DBpediaResourceOccurrence] = {
        val squaredConfidence = confidence*confidence
        val simThreshold = simThresholdList(math.max(((simThresholdList.length-1)*confidence).round.toInt, 0))

        occs.filter(occ => {
            if (occ.similarityScore < simThreshold) {
                SpotlightLog.info(this.getClass, "filtered out by similarity score threshold (%.2f<%f): %s", occ.similarityScore, simThreshold, occ, this.getClass)
                false
            }
            else if (occ.percentageOfSecondRank > (1-squaredConfidence)) {
                SpotlightLog.info(this.getClass, "filtered out by threshold of second ranked percentage (%f>%f): %s", occ.percentageOfSecondRank, 1-squaredConfidence, occ)
                false
            }
            else {
                true
            }
        })

    }

    // filter by support
    def filterBySupport(occs : List[DBpediaResourceOccurrence], targetSupport : Int) : List[DBpediaResourceOccurrence] = {
        occs.filter(occ => {
            if (occ.resource.support < targetSupport) {
                SpotlightLog.info(this.getClass, "filtered out by support (%d<%d): %s", occ.resource.support, targetSupport, occ)
                false
            }
            else {
                true
            }
        })
    }



    // filter by type
    private def filterByType(occs : List[DBpediaResourceOccurrence], dbpediaTypes : List[OntologyType], blacklistOrWhitelist : ListColor) : List[DBpediaResourceOccurrence] = {
        if (dbpediaTypes.filter(_.typeID.trim.nonEmpty).isEmpty) {
            SpotlightLog.info(this.getClass, "types are empty: showing all types")
            return occs
        }

        val acceptable = blacklistOrWhitelist match {
            case Whitelist => (resource : DBpediaResource) => {
                resource.types.filter(given => {
                    dbpediaTypes.find(listed => given equals listed) != None }
                ).nonEmpty
            }
            case Blacklist => (resource : DBpediaResource) => {
                resource.types.filter(given => {
                    dbpediaTypes.find(listed => given equals listed) != None }
                ).isEmpty
            }
        }

        val showUntyped = dbpediaTypes.find(t => DBpediaType.UNKNOWN equals t) != None
        occs.filter(occ => {
            // if the resource does not have type and the mainResources contain "unknown": don't filter
            if (showUntyped && occ.resource.types.isEmpty) {
                true
            }
            else {
                if (acceptable(occ.resource)) {
                    true
                }
                else {
                    SpotlightLog.info(this.getClass, "filtered out by %s: %s; list=%s", blacklistOrWhitelist, occ.resource, dbpediaTypes.map(_.typeID).mkString("List(", ",", ")"))
                    false
                }
            }
        })
    }


    /**
     * Get list from a SPARQL query and then blacklist or whitelist it.
     * Will execute the SPARQL query everytime you call this.
     * Best is to execute the query once and just call filterByBlacklist or filterByWhitelist.
     * We only leave the option of calling filterBySparql for use cases dealing with dynamic data in the SPARQL endpoint.
     */
    def filterBySparql(occs : List[DBpediaResourceOccurrence], sparqlQuery: String, blacklistOrWhitelist : ListColor, executer : SparqlQueryExecuter) : List[DBpediaResourceOccurrence] = {

        val uriSet = executer.query(sparqlQuery).toSet;
        SpotlightLog.debug(this.getClass, "SPARQL %s:%s", blacklistOrWhitelist, uriSet)

        val acceptable = blacklistOrWhitelist match {
            case Whitelist => (resource : DBpediaResource) =>  uriSet.contains(resource.uri)
            case Blacklist => (resource : DBpediaResource) => !uriSet.contains(resource.uri)
        }

        occs.filter(occ => {
            if (acceptable(occ.resource)) {
                true
            }
            else {
                SpotlightLog.info(this.getClass, "filtered out by SPARQL %s: %s", blacklistOrWhitelist, occ.resource)
                false
            }
        })
    }



    def main(args: Array[String]) {

//        val baseDir: String = "/home/pablo/eval/"
//        val inputFile: File = new File(baseDir+"Test.txt");
//        val plainText = scala.io.Source.fromFile(inputFile).mkString
        val plainText = "Presidents [[Obama]], [[Jim Bacon]] called political philosophy a [[Jackson]], arguing that the policy provides more generous assistance. bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla "

        var config = new SpotlightConfiguration("conf/server.properties");

        val factory = new SpotlightFactory(config);
        val disambiguator : Disambiguator = new DefaultDisambiguator(factory.contextSearcher)

//        // -- Spotter --
//        val spotter : Spotter = new LingPipeSpotter(spotterFile)
//
//        SpotlightLog.info(this.getClass, "Spotting...")
//        val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = spotter.extract(new Text(plainText))

        val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = ParseSurfaceFormText.parse(plainText)

        //SpotlightLog.info(this.getClass, "Selecting candidates...")
        //val selectedSpots = disambiguator.spotProbability(spottedSurfaceForms);
        val selectedSpots = spottedSurfaceForms;

        import scala.collection.JavaConversions._
        SpotlightLog.info(this.getClass, "Disambiguating... (%s)", disambiguator.name)
        val disambiguatedOccurrences : java.util.List[DBpediaResourceOccurrence] = disambiguator.disambiguate(selectedSpots)
        val occurrences = asBuffer(disambiguatedOccurrences).toList

        SpotlightLog.info(this.getClass, "Filtering... ")

        val query = "select distinct ?pol where {?pol a <http://dbpedia.org/ontology/President> .   FILTER REGEX(?pol, \"Bacon\") }";
        println(java.net.URLEncoder.encode(query))
        val filter = new AnnotationFilter(config);
        val filteredOccList : List[DBpediaResourceOccurrence] = filter.filter(occurrences, 0, 0, List(), query, false, true);

        //filteredOccList = AnnotationFilter.filterBySparql(occurrences, query, Whitelist)

        for (occ <- filteredOccList) {
            System.out.println("Entity:"+occ.resource);
        }
        SpotlightLog.info(this.getClass, "Done.")

        SpotlightLog.info(this.getClass, filteredOccList.mkString("\n"))

    }

}
