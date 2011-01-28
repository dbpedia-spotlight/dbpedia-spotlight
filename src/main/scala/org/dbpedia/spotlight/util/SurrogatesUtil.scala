package org.dbpedia.spotlight.util

import io.Source
import org.apache.commons.logging.LogFactory
import java.io._
import org.dbpedia.spotlight.model.{SurfaceForm, DBpediaResource}
import java.util.Scanner
import org.semanticweb.yars.nx.{Literal, Resource, Triple}
import org.semanticweb.yars.nx.parser.NxParser
import org.dbpedia.spotlight.string.ModifiedWikiUtil

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 11.08.2010
 * Time: 15:55:38
 * Functions to create Concept URIs (possible targets of disambiguations)
 *                     preferred URI mappings (i.e. transitive closure of redirects that end at Concept URIs)
 *                     surface forms for Concept URIs
 * from DBpedia data sets and Wikipedia.
 */


object SurrogatesUtil
{
    private val LOG = LogFactory.getLog(this.getClass)

    val MAXIMUM_SURFACE_FORM_LENGTH = 50


    //TODO make these files configurable in config file

    // DBpedia input
    var titlesFileName          = ConfigProperties.get("labelsDataset")
    var redirectsFileName       = ConfigProperties.get("redirectsDataset")
    var disambiguationsFileName = ConfigProperties.get("disambiguationsDataset")

    // output
    var conceptURIsFileName     = ConfigProperties.get("conceptURIs")
    var preferredURIsFileName   = ConfigProperties.get("preferredURIs")
    var surfaceFormsFileName    = ConfigProperties.get("surfaceForms")


    def saveConceptURIs(conceptURIsFile : File) {
        if (!new File(titlesFileName).isFile || !new File(redirectsFileName).isFile || !new File(disambiguationsFileName).isFile) {
            throw new IllegalStateException("labels, redirects or disambiguations file not set")
        }
        
        val badURIsFile = conceptURIsFile+".NOT"
        LOG.info("Creating concept URIs file "+conceptURIsFile+" ...")

        val conceptURIStream = new PrintStream(conceptURIsFile, "UTF-8")
        val badURIStream = new PrintStream(badURIsFile, "UTF-8")
        var badURIs = Set[String]()

        LOG.info("  collecting bad URIs from redirects in "+redirectsFileName+" and disambiguations in "+disambiguationsFileName+" ...")
        // redirects and disambiguations are bad URIs
        for (fileName <- List(redirectsFileName, disambiguationsFileName)) {
            val parser = new NxParser(new FileInputStream(fileName))
            while (parser.hasNext) {
                val triple = parser.next
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
        this.conceptURIsFileName = conceptURIsFile.getAbsolutePath
        ConfigProperties.set("conceptURIs", this.conceptURIsFileName)
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



    def savePreferredURIs(preferredURIsFile : File) {
        if (!new File(redirectsFileName).isFile) {
            throw new IllegalStateException("redirects file not set")
        }
        if (!new File(conceptURIsFileName).isFile) {
            throw new IllegalStateException("concept URIs not created yet; call saveConceptURIs first or set concept URIs file")
        }
        LOG.info("Creating preferred URIs map file "+preferredURIsFile+" ...")

        LOG.info("  loading concept URIs from "+conceptURIsFileName+"...")
        val conceptURIs = Source.fromFile(conceptURIsFileName, "UTF-8").getLines.toSet

        LOG.info("  loading redirects from "+redirectsFileName+"...")
        var linkMap = Map[String,String]()
        val parser = new NxParser(new FileInputStream(redirectsFileName))
        while (parser.hasNext) {
            val triple = parser.next
            val subj = triple(0).toString.replace("http://dbpedia.org/resource/", "")
            val obj = triple(2).toString.replace("http://dbpedia.org/resource/", "")
            linkMap = linkMap.updated(subj, obj)
        }

        val preferredURIsStream = new PrintStream(preferredURIsFile, "UTF-8")

        LOG.info("  collecting preferred URIs...")
        for (redirectUri <- linkMap.keys) {
            val endUri = getEndOfChainUri(linkMap, redirectUri)
            if (conceptURIs contains endUri)
                preferredURIsStream.println(redirectUri+"\t"+endUri)
        }

        preferredURIsStream.close
        LOG.info("Done.")
        this.preferredURIsFileName = preferredURIsFile.getAbsolutePath
        ConfigProperties.set("preferredURIs", this.preferredURIsFileName)
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
        if (surfaceForm.length > MAXIMUM_SURFACE_FORM_LENGTH)
            return false
        // contains a letter
        if ("""^[\W\d]+$""".r.findFirstIn(surfaceForm) != None)
            return false
        // contains a non-stopWord
        if (stopWords.nonEmpty
                && surfaceForm.split(" ").filterNot(
                    sfWord => stopWords.map(stopWord => stopWord.toLowerCase)
                              contains
                              sfWord.toLowerCase
                ).isEmpty)
            return false
        true
    }



    def saveSurfaceForms(surfaceFormsFile : File, stopWords : Set[String], lowerCased : Boolean=false) {
        if (!new File(redirectsFileName).isFile || !new File(disambiguationsFileName).isFile) {
            throw new IllegalStateException("redirects or disambiguations file not set")
        }
        if (!new File(conceptURIsFileName).isFile) {
            throw new IllegalStateException("concept URIs not created yet; call saveConceptURIs first or set concept URIs file")
        }

        LOG.info("Creating surface forms file "+surfaceFormsFile+" ...")
        LOG.info("  loading concept URIs from "+conceptURIsFileName+"...")
        val conceptURIs = Source.fromFile(conceptURIsFileName, "UTF-8").getLines.toSet

        LOG.info("  storing titles of concept URIs...")
        val surfaceFormsStream = new PrintStream(surfaceFormsFile, "UTF-8")
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
                val subj = triple(0).toString.replace("http://dbpedia.org/resource/", "")
                val obj = triple(2).toString.replace("http://dbpedia.org/resource/", "")
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
        this.surfaceFormsFileName = surfaceFormsFile.getAbsolutePath
        ConfigProperties.set("surfaceForms", this.surfaceFormsFileName)
    }

    // Returns a cleaned surface form if it is considered to be worth keeping
    def getCleanSurfaceForm(surfaceForm : String, stopWords : Set[String], lowerCased : Boolean=false) : Option[String] = {
        var cleanedSurfaceForm = ModifiedWikiUtil.wikiDecode(surfaceForm)
        cleanedSurfaceForm = cleanedSurfaceForm.replaceAll(""" \(.+?\)$""", "")
        cleanedSurfaceForm = cleanedSurfaceForm.replaceAll("""^(The|THE|A) """, "")
        //TODO also clean quotation marks??
        if (lowerCased)
            cleanedSurfaceForm = cleanedSurfaceForm.toLowerCase
        if (isGoodSurfaceForm(cleanedSurfaceForm, stopWords))
            Some(cleanedSurfaceForm)
        else
            None
    }

    def getCleanSurfaceForm(surfaceForm : String, lowerCased : Boolean) : Option[String] = {
        getCleanSurfaceForm(surfaceForm, Set[String](), lowerCased)
    }

    def getCleanSurfaceForm(surfaceForm : String) : Option[String] = {
        getCleanSurfaceForm(surfaceForm, Set[String](), false)
    }

    def getSurfaceForm(resource : DBpediaResource) : SurfaceForm = {
        new SurfaceForm(ModifiedWikiUtil.wikiDecode(resource.uri)
                        .replaceAll(""" \(.+?\)$""", "")
                        .replaceAll("""^(The|A) """, ""))
    }


    // map from URI to list of surface forms
    // used by IndexEnricher
    def getReverseSurrogatesMap_java(surrogatesFile : File, lowerCased : Boolean=false) : java.util.Map[String, java.util.LinkedHashSet[SurfaceForm]] = {
        LOG.info("Getting reverse surrogate mapping...")
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
            val subj = new Resource("http://dbpedia.org/resource/"+elements(1))
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
            val subj = new Resource("http://dbpedia.org/resource/"+elements(1))
            val obj = new Resource("http://lexvo.org/id/term/"+langString+"/"+ModifiedWikiUtil.wikiEncode(elements(0)))
            val triple = new Triple(subj, predicate, obj)
            ntStream.println(triple.toN3)
        }
        ntStream.close
        LOG.info("Done.")
    }


    def main(args : Array[String]) {
        // output
        val conceptURIsFile = new File("e:/dbpa/data/conceptURIs.list")
        val preferredURIsFile = new File("e:/dbpa/data/preferredURIs.tsv")
        val surfaceFormsTSVFile = new File("e:/dbpa/data/surface_forms/surface_forms-Wikipedia-TitRedDis.tsv")

        // get concept URIs
        saveConceptURIs(conceptURIsFile)

        // get preferred URIs
        savePreferredURIs(preferredURIsFile)

        // get surface forms 
        val stopWordsFileName = new File("e:/dbpa/data/surface_forms/stopwords.list")
        val stopWords = Source.fromFile(stopWordsFileName, "UTF-8").getLines.toSet
        saveSurfaceForms(surfaceFormsTSVFile, stopWords)


        // export to NT format (DBpedia and Lexvo.org)
        val dbpediaSurfaceFormsNTFileName = new File("e:/dbpa/data/surface_forms/surface_forms-Wikipedia-TitRedDis.nt")
        val lexvoSurfaceFormsNTFileName   = new File("e:/dbpa/data/surface_forms/lexicalizations-Wikipedia-TitRedDis.nt")
        exportSurfaceFormsTsvToDBpediaNt(dbpediaSurfaceFormsNTFileName)
        exportSurfaceFormsTsvToLexvoNt(lexvoSurfaceFormsNTFileName)
    }

}