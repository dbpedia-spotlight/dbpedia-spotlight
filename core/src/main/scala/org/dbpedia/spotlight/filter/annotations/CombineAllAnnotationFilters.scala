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

package org.dbpedia.spotlight.filter.annotations

import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.sparql.SparqlQueryExecuter
import scala.collection.JavaConversions._
import scalaj.collection.Imports._

class CombineAllAnnotationFilters(val config: SpotlightConfiguration) {

    private val LOG = LogFactory.getLog(this.getClass)

    // List of similarity scores from a development test run. Used to map confidence onto similarity score thresholds
    //val simThresholdList = List(0.875155268207,1.13201455044,1.23704027675,1.31043336244,1.37249636953,1.42678636431,1.47426818611,1.51699219814,1.5589122902,1.59833069873,1.63325972393,1.66743306586,1.7028011287,1.73513348674,1.7694202019,1.80213153211,1.83303477587,1.86295521865,1.89424885596,1.92353352553,1.95351725373,1.9834741473,2.01226448667,2.04107695223,2.0709070281,2.09985587688,2.12969398628,2.1595095078,2.18906425694,2.21847120257,2.24710029526,2.27635654737,2.30462947212,2.33447480288,2.36373952642,2.39334667746,2.4242092477,2.45480585618,2.48515738333,2.51675826106,2.5474261013,2.5797234845,2.61152222498,2.64228961515,2.67497596637,2.70977720029,2.74521420569,2.77768978363,2.81286198864,2.8489015388,2.88619747049,2.92265128786,2.95716825934,2.99291319769,3.03089856712,3.07175352309,3.11190546662,3.15373792839,3.19605117701,3.24065431276,3.28548179175,3.33270072677,3.38246673291,3.42992931096,3.47811829546,3.52977257069,3.58448599601,3.63959105997,3.69651854579,3.75468738153,3.81389593342,3.87974540897,3.94348987045,4.01026528803,4.08429597467,4.16200482218,4.24415476482,4.33359968944,4.4205907434,4.51784007216,4.61750527346,4.71220335034,4.82202004867,4.94565828569,5.07801206921,5.218870047,5.37478448951,5.54666888346,5.72957153978,5.91928811601,6.14764451981,6.40239167862,6.70676075179,7.06314127501,7.48235848422,7.91990547042,8.36265466001,8.83694069442,9.39367907709,10.4033660975)
    private val simThresholdList = config.getSimilarityThresholds.map(_.doubleValue).toList

    // Responsible for sending SPARQL queries to the endpoint (results will be used for filtering)
    private val sparqlExecuter = new SparqlQueryExecuter(config.getSparqlMainGraph(), config.getSparqlEndpoint())


    def filter(sfToCandidates: java.util.Map[SurfaceFormOccurrence, java.util.List[DBpediaResourceOccurrence]],
               confidence : Double,
               targetSupport : Int,
               dbpediaTypes : java.util.List[OntologyType],
               sparqlQuery : String,
               listColor : FilterPolicy.ListColor,
               coreferenceResolution : Boolean) : java.util.Map[SurfaceFormOccurrence,java.util.List[DBpediaResourceOccurrence]] = {
        sfToCandidates.toMap[SurfaceFormOccurrence,java.util.List[DBpediaResourceOccurrence]].map{
            case (sfOcc, candidateOccs) => {
                (sfOcc -> filter(candidateOccs,confidence,targetSupport,dbpediaTypes,sparqlQuery,listColor,coreferenceResolution))
            }
        }
    }

    def filter(occs : Traversable[DBpediaResourceOccurrence],
               confidence : Double,
               targetSupport : Int,
               dbpediaTypes : java.util.List[OntologyType],
               sparqlQuery : String,
               listColor : FilterPolicy.ListColor,
               coreferenceResolution : Boolean) : List[DBpediaResourceOccurrence] = {

        var filterList = (new ConfidenceFilter(simThresholdList, confidence)
                       :: new SupportFilter(targetSupport)
                       :: new TypeFilter(dbpediaTypes.toList, listColor)
                       :: new SparqlFilter(sparqlExecuter, sparqlQuery, listColor)
                       :: Nil)
        if (coreferenceResolution) filterList = new CoreferenceFilter :: filterList

        val filteredOccs = filterList.foldLeft(occs){ (o, f) => f.filterOccs(o) }

        filteredOccs.toList.sortBy(_.textOffset)
    }

    def filter(occs : java.util.List[DBpediaResourceOccurrence],
               confidence : Double,
               targetSupport : Int,
               dbpediaTypes : java.util.List[OntologyType],
               sparqlQuery : String,
               listColor : FilterPolicy.ListColor,
               coreferenceResolution : Boolean) : java.util.List[DBpediaResourceOccurrence] = {

        filter(occs.toList, confidence, targetSupport, dbpediaTypes, sparqlQuery, listColor, coreferenceResolution)
    }

    def filter(occs : Traversable[DBpediaResourceOccurrence],
               confidence : Double,
               targetSupport : Int,
               dbpediaTypes : java.util.List[OntologyType],
               sparqlQuery : String,
               blacklist : Boolean,
               coreferenceResolution : Boolean): List[DBpediaResourceOccurrence] = {

        val listColor = if(blacklist) FilterPolicy.Blacklist else FilterPolicy.Whitelist
        filter(occs, confidence, targetSupport, dbpediaTypes, sparqlQuery, listColor, coreferenceResolution)
    }

    def filterJ(occs : java.util.List[DBpediaResourceOccurrence],
               confidence : Double,
               targetSupport : Int,
               dbpediaTypes : java.util.List[OntologyType],
               sparqlQuery : String,
               blacklist : Boolean,
               coreferenceResolution : Boolean) : java.util.List[DBpediaResourceOccurrence] = {

        filter(occs.toList, confidence, targetSupport, dbpediaTypes, sparqlQuery, blacklist, coreferenceResolution)
    }

    def filter(occs : java.util.List[DBpediaResourceOccurrence],
               confidence : Double,
               targetSupport : Int,
               dbpediaTypes : List[OntologyType],
               sparqlQuery : String,
               blacklist : Boolean,
               coreferenceResolution : Boolean) : java.util.List[DBpediaResourceOccurrence] = {

        filterJ(occs, confidence, targetSupport, dbpediaTypes.asJava, sparqlQuery, blacklist, coreferenceResolution)
    }

}