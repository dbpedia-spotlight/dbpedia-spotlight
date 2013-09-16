/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.util


import io.Source
import org.dbpedia.spotlight.log.SpotlightLog
import java.io._
import org.dbpedia.spotlight.model.{Factory, SpotlightConfiguration, SurfaceForm}
import java.util.Scanner
import org.semanticweb.yars.nx.parser.NxParser
import org.semanticweb.yars.nx.{Literal, Resource, Triple}
import collection.JavaConversions._
import util.matching.Regex
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.dbpedia.extraction.util.WikiUtil

/**
 * Functions to create Concept URIs (possible mainResources of disambiguations)
 *                     transitive closure of redirects that end at Concept URIs
 *                     surface forms for Concept URIs
 * from DBpedia data sets and Wikipedia.
 *
 * Contains logic of what to index wrt. URIs and SurfaceForms.
 *
 *
 * @author maxjakob
 * @author pablomendes (created blacklisted URI patterns for language-specific stuff (e.g. List_of, etc.)
 */
object ExtractCandidateMap
{
    var maximumSurfaceFormLength = 50

    // DBpedia input
    var titlesFileName          = ""
    var redirectsFileName       = ""
    var disambiguationsFileName = ""
    var blacklistedURIPatterns = Set[Regex]()

    // output
    var conceptURIsFileName     = ""
    var redirectTCFileName      = ""
    var surfaceFormsFileName    = ""


    def saveConceptURIs {
        if (!new File(titlesFileName).isFile || !new File(redirectsFileName).isFile || !new File(disambiguationsFileName).isFile) {
            throw new IllegalStateException("labels, redirects or disambiguations file not set")
        }
        
        val badURIsFile = conceptURIsFileName+".NOT"
        SpotlightLog.info(this.getClass, "Creating concept URIs file %s ...", conceptURIsFileName)

        val conceptURIStream = new PrintStream(conceptURIsFileName, "UTF-8")
        val badURIStream = new PrintStream(badURIsFile, "UTF-8")
        var badURIs = Set[String]()

        SpotlightLog.info(this.getClass, "  collecting bad URIs from redirects in %s and disambiguations in %s ...", redirectsFileName, disambiguationsFileName)
        // redirects and disambiguations are bad URIs
        for (fileName <- List(redirectsFileName, disambiguationsFileName)) {
            val input = new BZip2CompressorInputStream(new FileInputStream(fileName),true)
            for(triple <- new NxParser(input)) {
                val badUri = triple(0).toString.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "")
                badURIs += badUri
                badURIStream.println(badUri)
            }
            input.close()
        }
        badURIStream.close

        SpotlightLog.info(this.getClass, "  collecting concept URIs from titles in %s, without redirects and disambiguations...", titlesFileName)
        val titlesInputStream = new BZip2CompressorInputStream(new FileInputStream(titlesFileName), true)
        // get titles without bad URIs
        val parser = new NxParser(titlesInputStream)
        while (parser.hasNext) {
            val triple = parser.next
            val uri = triple(0).toString.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "")
            if (looksLikeAGoodURI(uri) && !badURIs.contains(uri))
                conceptURIStream.println(uri)
        }
        conceptURIStream.close
        titlesInputStream.close

        SpotlightLog.info(this.getClass, "Done.")
//        conceptURIsFileName = conceptURIsFile.getAbsolutePath
//        IndexConfiguration.set("conceptURIs", conceptURIsFileName)
    }

    private def looksLikeAGoodURI(uri : String) : Boolean = {
        // cannot contain a slash (/)
        if (uri contains "/")
            return false
        // cannot contain a hash (#)
        if (uri contains "%23") //TODO re-evaluate this decision in context of DBpedia 3.7
            return false
        // has to contain a letter
        if ("""^[\\W\\d]+$""".r.findFirstIn(uri) != None)
            return false
        // cannot be a list, or any other pattern specified in the blacklist
        blacklistedURIPatterns.foreach(p => if (p.pattern.matcher(uri).matches) return false) // generalizes: if (uri contains "List_of_") return false
        true
    }



    def saveRedirectsTransitiveClosure {
        if (!new File(redirectsFileName).isFile) {
            throw new IllegalStateException("redirects file not set")
        }
        if (!new File(conceptURIsFileName).isFile) {
            throw new IllegalStateException("concept URIs not created yet; call saveConceptURIs first or set concept URIs file")
        }
        SpotlightLog.info(this.getClass, "Creating redirects transitive closure file %s ...", redirectsFileName)

        SpotlightLog.info(this.getClass, "  loading concept URIs from %s...", conceptURIsFileName)
        val conceptURIs = Source.fromFile(conceptURIsFileName, "UTF-8").getLines.toSet

        SpotlightLog.info(this.getClass, "  loading redirects from %s...", redirectsFileName)
        var linkMap = Map[String,String]()
        val redirectsInput = new BZip2CompressorInputStream(new FileInputStream(redirectsFileName), true)

        val parser = new NxParser(redirectsInput)
        while (parser.hasNext) {
            val triple = parser.next
            val subj = triple(0).toString.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "")
            val obj = triple(2).toString.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "")
            linkMap = linkMap.updated(subj, obj)
        }
        redirectsInput.close()

        val redURIstream = new PrintStream(redirectTCFileName, "UTF-8")

        SpotlightLog.info(this.getClass, "  collecting redirects transitive closure...")
        for (redirectUri <- linkMap.keys) {
            val endUri = getEndOfChainUri(linkMap, redirectUri)
            if (conceptURIs contains endUri) {
                redURIstream.println(redirectUri+"\t"+endUri)
            }
        }

        redURIstream.close
        SpotlightLog.info(this.getClass, "Done.")
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
        if (surfaceForm.length > maximumSurfaceFormLength) {
            return false
        }
        // contains a letter
        if ("""^[\W\d]+$""".r.findFirstIn(surfaceForm) != None) {
            return false
        }
        // not an escaped char. see http://sourceforge.net/mailarchive/message.php?msg_id=28908255
        if ("""\\\w""".r.findFirstIn(surfaceForm) != None) {
            return false
        }
        // contains a non-stopWord  //TODO Remove when case sensitivity and common-word/idiom detection is in place. This restriction will eliminate many works (books, movies, etc.) like Just_One_Fix, We_Right_Here, etc.
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

        SpotlightLog.info(this.getClass, "Creating surface forms file %s ...", surfaceFormsFileName)

        SpotlightLog.info(this.getClass, "  storing titles of concept URIs...")
        var conceptURIs = Set[String]()
        val surfaceFormsStream = new PrintStream(surfaceFormsFileName, "UTF-8")
        // all titles of concept URIs are surface forms
        for (conceptUri <- Source.fromFile(conceptURIsFileName, "UTF-8").getLines) {
            getCleanSurfaceForm(conceptUri, stopWords, lowerCased) match {
                case Some(sf : String) => surfaceFormsStream.println(sf+"\t"+conceptUri)
                case None => SpotlightLog.debug(this.getClass, "    concept URI %s' does not decode to a good surface form", conceptUri)
            }
            conceptURIs += conceptUri
        }

        SpotlightLog.info(this.getClass, "  storing titles of redirect and disambiguation URIs...")
        for (fileName <- List(redirectsFileName, disambiguationsFileName)) {
            val input = new BZip2CompressorInputStream(new FileInputStream(fileName), true)
            val parser = new NxParser(input)
            while (parser.hasNext) {
                val triple = parser.next
                val surfaceFormUri = triple(0).toString.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "")
                val uri = triple(2).toString.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "")

                if (conceptURIs contains uri) {
                    getCleanSurfaceForm(surfaceFormUri, stopWords, lowerCased) match {
                        case Some(sf : String) => surfaceFormsStream.println(sf+"\t"+uri)
                        case None =>
                    }
                }
            }
            input.close()
        }

        surfaceFormsStream.close
        SpotlightLog.info(this.getClass, "Done.")
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

        SpotlightLog.info(this.getClass, "Creating surface forms file %s ...", surfaceFormsFileName)
        SpotlightLog.info(this.getClass, "  loading concept URIs from %s...", conceptURIsFileName)
        val conceptURIs = Source.fromFile(conceptURIsFileName, "UTF-8").getLines.toSet

        SpotlightLog.info(this.getClass, "  storing titles of concept URIs...")
        val surfaceFormsStream = new PrintStream(surfaceFormsFileName, "UTF-8")
        // all titles of concept URIs are surface forms
        for (conceptUri <- conceptURIs) {
            getCleanSurfaceForm(conceptUri, stopWords, lowerCased) match {
                case Some(sf : String) => surfaceFormsStream.println(sf+"\t"+conceptUri)
                case None =>
            }
        }

        SpotlightLog.info(this.getClass, "  making map from %s and %s...", redirectsFileName, disambiguationsFileName)
        // make reverse map of redirects and disambiguations
        var linkMap = Map[String,List[String]]()
        for (fileName <- List(redirectsFileName, disambiguationsFileName)) {
            val input = new BZip2CompressorInputStream(new FileInputStream(fileName), true)
            val parser = new NxParser(input)
            while (parser.hasNext) {
                val triple = parser.next
                val subj = triple(0).toString.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "")
                val obj = triple(2).toString.replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "")
                linkMap = linkMap.updated(obj, linkMap.get(obj).getOrElse(List[String]()) ::: List(subj))
            }
            input.close()
        }

        SpotlightLog.info(this.getClass, "  collecting surface forms from map...")
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
        SpotlightLog.info(this.getClass, "Done.")
//        surfaceFormsFileName = surfaceFormsFile.getAbsolutePath
//        IndexConfiguration.set("surfaceForms", surfaceFormsFileName)
    }

    // Returns a cleaned surface form if it is considered to be worth keeping
    def getCleanSurfaceForm(surfaceForm : String, stopWords : Set[String], lowerCased : Boolean=false) : Option[String] = {
        val cleanedSurfaceForm = Factory.SurfaceForm.fromWikiPageTitle(surfaceForm, lowerCased).name
        if (isGoodSurfaceForm(cleanedSurfaceForm, stopWords)) Some(cleanedSurfaceForm) else None
    }

    // map from URI to list of surface forms
    // used by IndexEnricher
    def getSurfaceFormsMap(surrogatesFile : File, lowerCased : Boolean=false) : Map[String, List[SurfaceForm]] = {
        SpotlightLog.info(this.getClass, "Getting reverse surrogate mapping...")
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
        SpotlightLog.info(this.getClass, "Done.")
        reverseMap
    }

    def exportSurfaceFormsTsvToDBpediaNt(ntFile : File) {
        if (!new File(surfaceFormsFileName).isFile) {
            throw new IllegalStateException("surface forms not created yet; call saveSurfaceForms first or set surface forms file")
        }

        val surfaceFormPredicate = "http://dbpedia.org/ontology/hasSurfaceForm"
        val predicate = new Resource(surfaceFormPredicate)

        SpotlightLog.info(this.getClass, "Exporting surface forms TSV file %s to dbpedia.org NT-file %s ...", surfaceFormsFileName, ntFile)
        val ntStream = new PrintStream(ntFile, "UTF-8")

        for (line <- Source.fromFile(surfaceFormsFileName, "UTF-8").getLines) {
            val elements = line.split("\t")
            val subj = new Resource(SpotlightConfiguration.DEFAULT_NAMESPACE+elements(1))
            val obj = new Literal(elements(0), "lang=" + SpotlightConfiguration.DEFAULT_LANGUAGE_I18N_CODE, Literal.STRING)
            val triple = new Triple(subj, predicate, obj)
            ntStream.println(triple.toN3)
        }
        ntStream.close
        SpotlightLog.info(this.getClass, "Done.")
    }

    def exportSurfaceFormsTsvToLexvoNt(ntFile : File) {
        if (!new File(surfaceFormsFileName).isFile) {
            SpotlightLog.warn(this.getClass, "surface forms not created yet; call saveSurfaceForms first or set surface forms file")
            return
        }
        val langString = "eng"
        val surfaceFormPredicate = "http://lexvo.org/id/lexicalization"
        val predicate = new Resource(surfaceFormPredicate)

        SpotlightLog.info(this.getClass, "Exporting surface forms TSV file %s to lexvo.org NT-file %s ...", surfaceFormsFileName, ntFile)
        val ntStream = new PrintStream(ntFile, "UTF-8")

        for (line <- Source.fromFile(surfaceFormsFileName, "UTF-8").getLines) {
            val elements = line.split("\t")
            val subj = new Resource(SpotlightConfiguration.DEFAULT_NAMESPACE+elements(1))
            val obj = new Resource("http://lexvo.org/id/term/"+langString+"/"+WikiUtil.wikiEncode(elements(0)))
            val triple = new Triple(subj, predicate, obj)
            ntStream.println(triple.toN3)
        }
        ntStream.close
        SpotlightLog.info(this.getClass, "Done.")
    }

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)
        val config = new IndexingConfiguration(indexingConfigFileName)

        val language = config.getLanguage().toLowerCase

        // DBpedia input
        titlesFileName          = config.get("org.dbpedia.spotlight.data.labels")
        redirectsFileName       = config.get("org.dbpedia.spotlight.data.redirects")
        disambiguationsFileName = config.get("org.dbpedia.spotlight.data.disambiguations")

        // output
        conceptURIsFileName     = config.get("org.dbpedia.spotlight.data.conceptURIs")
        redirectTCFileName      = config.get("org.dbpedia.spotlight.data.redirectsTC")
        surfaceFormsFileName    = config.get("org.dbpedia.spotlight.data.surfaceForms")

        maximumSurfaceFormLength = config.get("org.dbpedia.spotlight.data.maxSurfaceFormLength").toInt

        //DBpedia config
        SpotlightConfiguration.DEFAULT_NAMESPACE=config.get("org.dbpedia.spotlight.default_namespace",SpotlightConfiguration.DEFAULT_NAMESPACE)


        //Bad URIs -- will exclude any URIs that match these patterns. Used for Lists, disambiguations, etc.
        val blacklistedURIPatternsFileName = config.get("org.dbpedia.spotlight.data.badURIs."+language)
        blacklistedURIPatterns = Source.fromFile(blacklistedURIPatternsFileName).getLines.map( u => u.r ).toSet

        //Stopwords (bad surface forms)
        val stopWordsFileName = config.get("org.dbpedia.spotlight.data.stopWords."+language)
        val stopWords = Source.fromFile(stopWordsFileName, "UTF-8").getLines.toSet

        // get concept URIs
        saveConceptURIs

        // get redirects
        saveRedirectsTransitiveClosure

        // get "clean" surface forms, i.e. the ones obtained from TRDs
        saveSurfaceForms(stopWords)

        // TODO get "extra" surface forms from wikipedia occurrences. (see:
        //      should allow user to specify a minimum count threshold
        //      should perform redirectsTransitiveClosure for target URIs
        //saveExtraSurfaceForms

        //TODO create another class called CreateLexicalizationsDataset
        // export to NT format (DBpedia and Lexvo.org)
        //val dbpediaSurfaceFormsNTFileName = new File("e:/dbpa/data/surface_forms/surface_forms-Wikipedia-TitRedDis.nt")
        //val lexvoSurfaceFormsNTFileName   = new File("e:/dbpa/data/surface_forms/lexicalizations-Wikipedia-TitRedDis.nt")
        //exportSurfaceFormsTsvToDBpediaNt(dbpediaSurfaceFormsNTFileName)
        //exportSurfaceFormsTsvToLexvoNt(lexvoSurfaceFormsNTFileName)
    }

}