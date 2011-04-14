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

import io.Source
import org.apache.commons.logging.LogFactory
import java.io._
import org.dbpedia.spotlight.model.{SurfaceForm, DBpediaResource}
import java.util.Scanner
import org.semanticweb.yars.nx.parser.NxParser
import org.dbpedia.spotlight.string.ModifiedWikiUtil
import org.semanticweb.yars.nx.{Node, Literal, Resource, Triple}
import collection.JavaConversions._

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 11.08.2010
 * Time: 15:55:38
 * Functions to create Concept URIs (possible targets of disambiguations)
 *                     transitive closure of redirects that end at Concept URIs
 *                     surface forms for Concept URIs
 * from DBpedia data sets and Wikipedia.
 */


object SurrogatesUtil
{
    private val LOG = LogFactory.getLog(this.getClass)

    val MAXIMUM_SURFACE_FORM_LENGTH = 50

    // DBpedia input
    var titlesFileName          = ""
    var redirectsFileName       = ""
    var disambiguationsFileName = ""

    // output
    var conceptURIsFileName     = ""
    var redirectTCFileName      = ""
    var surfaceFormsFileName    = ""


    def saveConceptURIs {
        if (!new File(titlesFileName).isFile || !new File(redirectsFileName).isFile || !new File(disambiguationsFileName).isFile) {
            throw new IllegalStateException("labels, redirects or disambiguations file not set")
        }
        
        val badURIsFile = conceptURIsFileName+".NOT"
        LOG.info("Creating concept URIs file "+conceptURIsFileName+" ...")

        val conceptURIStream = new PrintStream(conceptURIsFileName, "UTF-8")
        val badURIStream = new PrintStream(badURIsFile, "UTF-8")
        var badURIs = Set[String]()

        LOG.info("  collecting bad URIs from redirects in "+redirectsFileName+" and disambiguations in "+disambiguationsFileName+" ...")
        // redirects and disambiguations are bad URIs
        for (fileName <- List(redirectsFileName, disambiguationsFileName)) {
            //val parser = new NxParser(new FileInputStream(fileName))
            //while (parser.hasNext) {
            //    val triple = parser.next
            for(triple <- new NxParser(new FileInputStream(fileName))) {
                val badUri = triple(0).toString.replace("http://dbpedia.org/resource/", "")
                badURIs += badUri
                badURIStream.println(badUri)
            }
        }
        badURIStream.close

        LOG.info("  collecting concept URIs from titles in "+titlesFileName+", without redirects and disambiguations...")
        // get titles without bad URIs
        val parser = new NxParser(new FileInputStream(titlesFileName))
        while (parser.hasNext) {
            val triple = parser.next
            val uri = triple(0).toString.replace("http://dbpedia.org/resource/", "")
            if (looksLikeAGoodURI(uri) && !badURIs.contains(uri))
                conceptURIStream.println(uri)
        }
        conceptURIStream.close

        LOG.info("Done.")
//        conceptURIsFileName = conceptURIsFile.getAbsolutePath
//        IndexConfiguration.set("conceptURIs", conceptURIsFileName)
    }

    private def looksLikeAGoodURI(uri : String) : Boolean = {
        if (uri contains "List_of_")
            return false
        if (uri contains "/")
            return false
        if (uri contains "%23") // #
            return false
        // contains a letter
        if ("""^[\\W\\d]+$""".r.findFirstIn(uri) != None)
            return false
        true
    }



    def saveRedirectsTransitiveClosure {
        if (!new File(redirectsFileName).isFile) {
            throw new IllegalStateException("redirects file not set")
        }
        if (!new File(conceptURIsFileName).isFile) {
            throw new IllegalStateException("concept URIs not created yet; call saveConceptURIs first or set concept URIs file")
        }
        LOG.info("Creating redirects transitive closure file "+redirectsFileName+" ...")

        LOG.info("  loading concept URIs from "+conceptURIsFileName+"...")
        val conceptURIs = Source.fromFile(conceptURIsFileName, "UTF-8").getLines.toSet

        LOG.info("  loading redirects from "+redirectsFileName+"...")
        var linkMap = Map[String,String]()
        val parser = new NxParser(new FileInputStream(redirectsFileName))
        while (parser.hasNext) {
            val triple = parser.next
            val subj = triple(0).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
            val obj = triple(2).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
            linkMap = linkMap.updated(subj, obj)
        }

        val redURIstream = new PrintStream(redirectTCFileName, "UTF-8")

        LOG.info("  collecting redirects transitive closure...")
        for (redirectUri <- linkMap.keys) {
            val endUri = getEndOfChainUri(linkMap, redirectUri)
            if (conceptURIs contains endUri) {
                redURIstream.println(redirectUri+"\t"+endUri)
            }
        }

        redURIstream.close
        LOG.info("Done.")
//        redirectTCFileName = redirectTCFileName.getAbsolutePath
//        IndexConfiguration.set("preferredURIs", redirectTCFileName)
    }

    private def getEndOfChainUri(m : Map[String,String], k : String) : String = {
        // get end of chain but check for redirects to itself
        m.get(k) match {
            case Some(s : String) => if (s equals k) k else getEndOfChainUri(m, s)
            case None => k
        }
    }


    def isGoodSurfaceForm(surfaceForm : String, stopWords : Set[String]) : Boolean = {
        // not longer than limit
        if (surfaceForm.length > MAXIMUM_SURFACE_FORM_LENGTH) {
            return false
        }
        // contains a letter
        if ("""^[\W\d]+$""".r.findFirstIn(surfaceForm) != None) {
            return false
        }
        // contains a non-stopWord
        if (stopWords.nonEmpty
                && surfaceForm.split(" ").filterNot(
                    sfWord => stopWords.map(stopWord => stopWord.toLowerCase)
                              contains
                              sfWord.toLowerCase
                ).isEmpty) {
            return false
        }
        true
    }


    def saveSurfaceForms(stopWords : Set[String], lowerCased : Boolean=false) {
        if (!new File(redirectsFileName).isFile || !new File(disambiguationsFileName).isFile) {
            throw new IllegalStateException("redirects or disambiguations file not set")
        }
        if (!new File(conceptURIsFileName).isFile) {
            throw new IllegalStateException("concept URIs not created yet; call saveConceptURIs first or set concept URIs file")
        }

        LOG.info("Creating surface forms file "+surfaceFormsFileName+" ...")

        LOG.info("  storing titles of concept URIs...")
        var conceptURIs = Set[String]()
        val surfaceFormsStream = new PrintStream(surfaceFormsFileName, "UTF-8")
        // all titles of concept URIs are surface forms
        for (conceptUri <- Source.fromFile(conceptURIsFileName, "UTF-8").getLines) {
            getCleanSurfaceForm(conceptUri, stopWords, lowerCased) match {
                case Some(sf : String) => surfaceFormsStream.println(sf+"\t"+conceptUri)
                case None => LOG.debug("    concept URI "+conceptUri+"' does not decode to a good surface form")
            }
            conceptURIs += conceptUri
        }

        LOG.info("  storing titles of redirect and disambiguation URIs...")
        for (fileName <- List(redirectsFileName, disambiguationsFileName)) {
            val parser = new NxParser(new FileInputStream(fileName))
            while (parser.hasNext) {
                val triple = parser.next
                val surfaceFormUri = triple(0).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
                val uri = triple(2).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")

                if (conceptURIs contains uri) {
                    getCleanSurfaceForm(surfaceFormUri, stopWords, lowerCased) match {
                        case Some(sf : String) => surfaceFormsStream.println(sf+"\t"+uri)
                        case None =>
                    }
                }
            }
        }

        surfaceFormsStream.close
        LOG.info("Done.")
//        surfaceFormsFileName = surfaceFormsFile.getAbsolutePath
//        IndexConfiguration.set("surfaceForms", surfaceFormsFileName)
    }


    def saveSurfaceForms_fromGraph(stopWords : Set[String], lowerCased : Boolean=false) {
        if (!new File(redirectsFileName).isFile || !new File(disambiguationsFileName).isFile) {
            throw new IllegalStateException("redirects or disambiguations file not set")
        }
        if (!new File(conceptURIsFileName).isFile) {
            throw new IllegalStateException("concept URIs not created yet; call saveConceptURIs first or set concept URIs file")
        }

        LOG.info("Creating surface forms file "+surfaceFormsFileName+" ...")
        LOG.info("  loading concept URIs from "+conceptURIsFileName+"...")
        val conceptURIs = Source.fromFile(conceptURIsFileName, "UTF-8").getLines.toSet

        LOG.info("  storing titles of concept URIs...")
        val surfaceFormsStream = new PrintStream(surfaceFormsFileName, "UTF-8")
        // all titles of concept URIs are surface forms
        for (conceptUri <- conceptURIs) {
            getCleanSurfaceForm(conceptUri, stopWords, lowerCased) match {
                case Some(sf : String) => surfaceFormsStream.println(sf+"\t"+conceptUri)
                case None =>
            }
        }

        LOG.info("  making map from "+redirectsFileName+" and "+disambiguationsFileName+"...")
        // make reverse map of redirects and disambiguations
        var linkMap = Map[String,List[String]]()
        for (fileName <- List(redirectsFileName, disambiguationsFileName)) {
            val parser = new NxParser(new FileInputStream(fileName))
            while (parser.hasNext) {
                val triple = parser.next
                val subj = triple(0).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
                val obj = triple(2).toString.replace(DBpediaResource.DBPEDIA_RESOURCE_PREFIX, "")
                linkMap = linkMap.updated(obj, linkMap.get(obj).getOrElse(List[String]()) ::: List(subj))
            }
        }

        LOG.info("  collecting surface forms from map...")
        for (currentURI <- linkMap.keys.filter(conceptURIs contains _)) {
            var linkedUris = List(currentURI)
            var cyclePrevention = List[String]()
            while (linkedUris.nonEmpty) {
                // save the decoded URI as surface form for the current concept URI
                getCleanSurfaceForm(linkedUris.head, stopWords, lowerCased) match {
                    case Some(sf : String) => surfaceFormsStream.println(sf+"\t"+currentURI)
                    case None =>
                }
                // get all redirects and disambiguations that link to the last URI
                // take care that there are no loops
                var linksList = linkMap.get(linkedUris.head).getOrElse(List[String]()).filterNot(cyclePrevention contains _)

                // add links that point here
                linkedUris = linkedUris.tail ::: linksList

                cyclePrevention = cyclePrevention ::: linksList
                for (i <- 0 to math.max(-1, cyclePrevention.length-10)) {
                    cyclePrevention = cyclePrevention.tail
                }
            }
        }

        surfaceFormsStream.close
        LOG.info("Done.")
//        surfaceFormsFileName = surfaceFormsFile.getAbsolutePath
//        IndexConfiguration.set("surfaceForms", surfaceFormsFileName)
    }

    // Returns a cleaned surface form if it is considered to be worth keeping
    def getCleanSurfaceForm(surfaceForm : String, stopWords : Set[String], lowerCased : Boolean=false) : Option[String] = {
        var cleanedSurfaceForm = ModifiedWikiUtil.wikiDecode(surfaceForm) //TODO MAX please prefer ModifiedWikiUtil.cleanSurfaceForm
        cleanedSurfaceForm = cleanedSurfaceForm.replaceAll(""" \(.+?\)$""", "")
        cleanedSurfaceForm = cleanedSurfaceForm.replaceAll("""^(The|THE|A) """, "")
        //TODO also clean quotation marks??
        if (lowerCased) {
            cleanedSurfaceForm = cleanedSurfaceForm.toLowerCase
        }
        if (isGoodSurfaceForm(cleanedSurfaceForm, stopWords)) {
            Some(cleanedSurfaceForm)
        }
        else {
            None
        }
    }

    def getCleanSurfaceForm(surfaceForm : String, lowerCased : Boolean) : Option[String] = {
        getCleanSurfaceForm(surfaceForm, Set[String](), lowerCased)
    }

    def getCleanSurfaceForm(surfaceForm : String) : Option[String] = {
        getCleanSurfaceForm(surfaceForm, Set[String](), false)
    }

    def getSurfaceForm(resource : DBpediaResource) : SurfaceForm = {
        new SurfaceForm(ModifiedWikiUtil.wikiDecode(resource.uri)  //TODO MAX please prefer ModifiedWikiUtil.cleanSurfaceForm
                        .replaceAll(""" \(.+?\)$""", "")
                        .replaceAll("""^(The|A) """, ""))
    }


    // map from URI to list of surface forms
    // used by IndexEnricher
    // uri -> list(sf1, sf2)
    def getSurfaceFormsMap_java(surrogatesFile : File, lowerCased : Boolean=false) : java.util.Map[String, java.util.LinkedHashSet[SurfaceForm]] = {
        LOG.info("Getting surface form map...")
        val reverseMap : java.util.Map[String, java.util.LinkedHashSet[SurfaceForm]] = new java.util.HashMap[String, java.util.LinkedHashSet[SurfaceForm]]()
        val separator = "\t"
        val tsvScanner = new Scanner(new FileInputStream(surrogatesFile), "UTF-8")
        while (tsvScanner.hasNextLine) {
            val line = tsvScanner.nextLine.split(separator)
            val sf = if (lowerCased) line(0).toLowerCase else line(0)
            val uri = line(1)
            var sfList = reverseMap.get(uri)
            if (sfList == null) {
                sfList = new java.util.LinkedHashSet[SurfaceForm]()
            }
            sfList.add(new SurfaceForm(sf))
            reverseMap.put(uri, sfList)
        }
        LOG.info("Done.")
        reverseMap
    }

    // map from URI to list of surface forms
    // used by IndexEnricher
    def getSurfaceFormsMap(surrogatesFile : File, lowerCased : Boolean=false) : Map[String, List[SurfaceForm]] = {
        LOG.info("Getting reverse surrogate mapping...")
        var reverseMap = Map[String, List[SurfaceForm]]()
        val separator = "\t"

        val tsvScanner = new Scanner(new FileInputStream(surrogatesFile), "UTF-8")
        for(line <- Source.fromFile(surrogatesFile, "UTF-8").getLines) {
            val el = tsvScanner.nextLine.split(separator)
            val sf = if (lowerCased) new SurfaceForm(el(0).toLowerCase) else new SurfaceForm(el(0))
            val uri = el(1)
            val sfList : List[SurfaceForm] = sf :: reverseMap.get(uri).getOrElse(List[SurfaceForm]())
            reverseMap = reverseMap.updated(uri, sfList)
        }
        LOG.info("Done.")
        reverseMap
    }

    def exportSurfaceFormsTsvToDBpediaNt(ntFile : File) {
        if (!new File(surfaceFormsFileName).isFile) {
            throw new IllegalStateException("surface forms not created yet; call saveSurfaceForms first or set surface forms file")
        }
        val langString = "en"
        val surfaceFormPredicate = "http://dbpedia.org/ontology/hasSurfaceForm"
        val predicate = new Resource(surfaceFormPredicate)

        LOG.info("Exporting surface forms TSV file "+surfaceFormsFileName+" to dbpedia.org NT-file "+ntFile+" ...")
        val ntStream = new PrintStream(ntFile, "UTF-8")

        for (line <- Source.fromFile(surfaceFormsFileName, "UTF-8").getLines) {
            val elements = line.split("\t")
            val subj = new Resource(DBpediaResource.DBPEDIA_RESOURCE_PREFIX+elements(1))
            val obj = new Literal(elements(0), langString, Literal.STRING)
            val triple = new Triple(subj, predicate, obj)
            ntStream.println(triple.toN3)
        }
        ntStream.close
        LOG.info("Done.")
    }

    def exportSurfaceFormsTsvToLexvoNt(ntFile : File) {
        if (!new File(surfaceFormsFileName).isFile) {
            LOG.warn("surface forms not created yet; call saveSurfaceForms first or set surface forms file")
            return
        }
        val langString = "eng"
        val surfaceFormPredicate = "http://lexvo.org/id/lexicalization"
        val predicate = new Resource(surfaceFormPredicate)

        LOG.info("Exporting surface forms TSV file "+surfaceFormsFileName+" to lexvo.org NT-file "+ntFile+" ...")
        val ntStream = new PrintStream(ntFile, "UTF-8")

        for (line <- Source.fromFile(surfaceFormsFileName, "UTF-8").getLines) {
            val elements = line.split("\t")
            val subj = new Resource(DBpediaResource.DBPEDIA_RESOURCE_PREFIX+elements(1))
            val obj = new Resource("http://lexvo.org/id/term/"+langString+"/"+ModifiedWikiUtil.wikiEncode(elements(0)))
            val triple = new Triple(subj, predicate, obj)
            ntStream.println(triple.toN3)
        }
        ntStream.close
        LOG.info("Done.")
    }


    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)
        val config = new IndexingConfiguration(indexingConfigFileName)

        // DBpedia input
        titlesFileName          = config.get("org.dbpedia.spotlight.data.labels")
        redirectsFileName       = config.get("org.dbpedia.spotlight.data.redirects")
        disambiguationsFileName = config.get("org.dbpedia.spotlight.data.disambiguations")

        // output
        conceptURIsFileName     = config.get("org.dbpedia.spotlight.data.conceptURIs")
        redirectTCFileName      = config.get("org.dbpedia.spotlight.data.redirectsTC")
        surfaceFormsFileName    = config.get("org.dbpedia.spotlight.data.surfaceForms")


        // get concept URIs
        saveConceptURIs

        // get redirects
        saveRedirectsTransitiveClosure

        // get surface forms 
        val stopWordsFileName = config.get("org.dbpedia.spotlight.data.stopWords")
        val stopWords = Source.fromFile(stopWordsFileName, "UTF-8").getLines.toSet
        saveSurfaceForms(stopWords)


        // export to NT format (DBpedia and Lexvo.org)
        val dbpediaSurfaceFormsNTFileName = new File("e:/dbpa/data/surface_forms/surface_forms-Wikipedia-TitRedDis.nt")
        val lexvoSurfaceFormsNTFileName   = new File("e:/dbpa/data/surface_forms/lexicalizations-Wikipedia-TitRedDis.nt")
        //exportSurfaceFormsTsvToDBpediaNt(dbpediaSurfaceFormsNTFileName)
        //exportSurfaceFormsTsvToLexvoNt(lexvoSurfaceFormsNTFileName)
    }

}