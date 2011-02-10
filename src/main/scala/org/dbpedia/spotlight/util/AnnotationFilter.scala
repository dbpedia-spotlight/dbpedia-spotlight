package org.dbpedia.spotlight.util

import org.apache.commons.logging.LogFactory
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.sparql.SparqlQueryExecuter
import java.io.File
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import org.dbpedia.spotlight.disambiguate.{DefaultDisambiguator, Disambiguator}
import org.dbpedia.spotlight.string.ParseSurfaceFormText


object AnnotationFilter
{
    private val LOG = LogFactory.getLog(this.getClass)

    val DEFAULT_CONFIDENCE : Double = 0.5
    val DEFAULT_SUPPORT : Int = 100
    val DEFAULT_TYPES : java.util.List[DBpediaType] = new java.util.LinkedList[DBpediaType]()
    val DEFAULT_SHOW_DUPLICATES = true
    val DEFAULT_COREFERENCE_RESOLUTION = true

    // set from a test run //TODO document this!
    val simThresholdList = List(0,
        0.1155594, 0.1413648, 0.1555880, 0.1666082, 0.1769609, 0.1866261, 0.1957517, 0.20482580, 0.2138903, 0.2237287,
        0.2335491, 0.2442384, 0.2560859, 0.2693643, 0.2848305, 0.3033198, 0.3288046, 0.36692468, 0.449684 , 0.5)

    val baseDir = "/home/pablo/data/" //TODO get this from config file
    //val simThresholdList = scala.io.Source.fromFile(baseDir+"failedTests.simScores").getLines().map(x => x.toDouble).toList.sorted
    //val simThresholdList = List(0.000173042368260212, 0.00437988666817546, 0.014439694583416, 0.0914923325181007, 0.146780446171761, 0.378425091505051, 22.6561012268066)

    object ListColor extends Enumeration {
        type ListColor = Value
        val Blacklist, Whitelist = Value
    }
    import ListColor._


    def filter(occs : java.util.List[DBpediaResourceOccurrence],
               confidence : Double=DEFAULT_CONFIDENCE,
               targetSupport : Int=DEFAULT_SUPPORT,
               dbpediaTypes : java.util.List[DBpediaType]=DEFAULT_TYPES,
               sparqlQuery : String = "",
               blacklist : Boolean = false,
               coreferenceResolution : Boolean=DEFAULT_COREFERENCE_RESOLUTION) : java.util.List[DBpediaResourceOccurrence] = {

        val filteredOccs = filter(occs.toList, confidence, targetSupport, dbpediaTypes, sparqlQuery, blacklist, coreferenceResolution)
        filteredOccs
    }

    def filter(occs : List[DBpediaResourceOccurrence],
               confidence : Double,
               targetSupport : Int,
               dbpediaTypes : java.util.List[DBpediaType],
               sparqlQuery : String,
               blacklist : Boolean,
               coreferenceResolution : Boolean): List[DBpediaResourceOccurrence] = {

        var filteredOccs = occs

        val listColor = if(blacklist) Blacklist else Whitelist

        if (coreferenceResolution) filteredOccs = buildCoreferents(filteredOccs)

        filteredOccs = filterBySupport(filteredOccs, targetSupport)
        if (0 <= confidence || confidence <= 1) {
            filteredOccs = filterByConfidence(filteredOccs, confidence)
        }
        else {
            LOG.warn("confidence must be between 0 and 1 (is "+confidence+"); setting to 0")
        }

        filteredOccs = filterByType(filteredOccs, dbpediaTypes.toList, listColor)
        filteredOccs = filterBySupport(filteredOccs, targetSupport)

        if(sparqlQuery != null && sparqlQuery != "") {
            filteredOccs = filterBySparql(filteredOccs, sparqlQuery, listColor)
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
                    LOG.info("found coreferent: "+laterOcc.surfaceForm+" at position "+laterOcc.textOffset+" probably coreferring to "+prevOcc.surfaceForm+" at position "+prevOcc.textOffset+"; copying "+prevOcc.resource)
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
        //                    LOG.info("filtered out as coreferent: "+laterOcc.surfaceForm+" at position "+laterOcc.textOffset+" probably coreferring to "+prevOcc.surfaceForm+" at position "+prevOcc.textOffset)
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
        val simThreshold = simThresholdList((simThresholdList.length*confidence).round.toInt)

        occs.filter(occ => {
            if (occ.similarityScore < simThreshold) {
                LOG.info("filtered out by similarity score threshold ("+"%.2f".format(occ.similarityScore)+"<"+simThreshold+"): "+occ)
                false
            }
            else if (occ.percentageOfSecondRank > (1-squaredConfidence)) {
                LOG.info("filtered out by threshold of second ranked percentage ("+occ.percentageOfSecondRank+">"+(1-squaredConfidence)+"): "+occ)
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
                LOG.info("filtered out by support ("+occ.resource.support+"<"+targetSupport+"): "+occ)
                false
            }
            else {
                true
            }
        })
    }

    // filter by type
    private def filterByType(occs : List[DBpediaResourceOccurrence], dbpediaTypes : List[DBpediaType], blacklistOrWhitelist : ListColor) : List[DBpediaResourceOccurrence] = {
        if (dbpediaTypes.filter(_.name.trim.nonEmpty).isEmpty) {
            LOG.info("types are empty: showing all types")
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

        val showUntyped = dbpediaTypes.find(t => "unknown" equals t.name) != None
        occs.filter(occ => {
            // if the resource does not have type and the targets contain "unknown": don't filter
            if (showUntyped && occ.resource.types.isEmpty) {
                true
            }
            else {
                if (acceptable(occ.resource)) {
                    true
                }
                else {
                    LOG.info("filtered out by type: "+occ.resource+" not part of "+blacklistOrWhitelist+ "types "+dbpediaTypes.map(_.name).mkString("List(", ",", ")"))
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
    def filterBySparql(occs : List[DBpediaResourceOccurrence], sparqlQuery: String, blacklistOrWhitelist : ListColor, executer : SparqlQueryExecuter = new SparqlQueryExecuter) : List[DBpediaResourceOccurrence] = {

        val uriSet = executer.query(sparqlQuery).toSet;
        LOG.debug("SPARQL "+blacklistOrWhitelist+":"+uriSet);

        val acceptable = blacklistOrWhitelist match {
            case Whitelist => (resource : DBpediaResource) =>  uriSet.contains(resource.uri)
            case Blacklist => (resource : DBpediaResource) => !uriSet.contains(resource.uri)
        }

        occs.filter(occ => {
            if (acceptable(occ.resource)) {
                true
            }
            else {
                LOG.info("filtered out by SPARQL "+blacklistOrWhitelist+": "+occ.resource)
                false
            }
        })
    }



    def main(args: Array[String]) {

//        val baseDir: String = "/home/pablo/eval/"
//        val inputFile: File = new File(baseDir+"Test.txt");
//        val plainText = scala.io.Source.fromFile(inputFile).mkString
        val plainText = "Presidents [[Obama]], [[Jim Bacon]] called political philosophy a [[Jackson]], arguing that the policy provides more generous assistance. bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla "

        val spotterFile    = new File("e:/dbpa/data/__smallSpotDictList.spotterDictionary");
        //val spotterFile    = new File("/home/pablo/eval/manual/Eval.spotterDictionary");
        val indexDir = new File("E:\\dbpa\\data\\index\\DisambigIndex.restrictedSFs.plusTypes-plusSFs");

        val disambiguator : Disambiguator = new DefaultDisambiguator(indexDir)

//        // -- Spotter --
//        val spotter : Spotter = new LingPipeSpotter(spotterFile)
//
//        LOG.info("Spotting...")
//        val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = spotter.extract(new Text(plainText))

        val spottedSurfaceForms : java.util.List[SurfaceFormOccurrence] = ParseSurfaceFormText.parse(plainText)

        //LOG.info("Selecting candidates...");
        //val selectedSpots = disambiguator.spotProbability(spottedSurfaceForms);
        val selectedSpots = spottedSurfaceForms;

        import scala.collection.JavaConversions._
        LOG.info("Disambiguating... ("+disambiguator.name+")")
        val disambiguatedOccurrences : java.util.List[DBpediaResourceOccurrence] = disambiguator.disambiguate(selectedSpots)
        val occurrences = asBuffer(disambiguatedOccurrences).toList

        LOG.info("Filtering... ")

        val query = "select distinct ?pol where {?pol a <http://dbpedia.org/ontology/President> .   FILTER REGEX(?pol, \"Bacon\") }";
        val filteredOccList : List[DBpediaResourceOccurrence] = AnnotationFilter.filter(occurrences, 0, 0, AnnotationFilter.DEFAULT_TYPES, query, false, AnnotationFilter.DEFAULT_COREFERENCE_RESOLUTION);

        //filteredOccList = AnnotationFilter.filterBySparql(occurrences, query, Whitelist)

        for (occ <- filteredOccList) {
            System.out.println("Entity:"+occ.resource);
        }
        LOG.info("Done.")

        LOG.info(filteredOccList.mkString("\n"));

    }

}
