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

package org.dbpedia.spotlight.lucene.index


import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.util.IndexingConfiguration
import java.util.Scanner
import java.io.FileInputStream
import org.dbpedia.spotlight.log.SpotlightLog
import scala.collection.JavaConverters._

/**
 * In our first implementation we used to index all anchor text found in Wikipedia as surface forms to a target URI.
 * We have since moved to indexing without surface forms, then pre-processing the anchors before indexing.
 * Some possible pre-processing steps: eliminating low count surface forms, adding acronyms, name variations, etc.
 * TODO think of some stemming to provide more alternative matches for a sf (e.g. http://www.mpi-inf.mpg.de/yago-naga/javatools/doc/javatools/parsers/PlingStemmer.html)
 *
 * @author maxjakob
 * @author pablomendes (alternative surface forms)
 */

object AddSurfaceFormsToIndex
{
    def toLowercase(sf: String, lowerCased: Boolean) : List[String] = {
        if (lowerCased) (sf.toLowerCase :: List(sf)) else List(sf)
    }

    def fromTitlesToAlternativesJ(sf: SurfaceForm) : java.util.List[SurfaceForm] = {
        fromTitlesToAlternatives(sf.name).map(s => new SurfaceForm(s)).toList.asJava
    }

    def fromTitlesToAlternatives(sf: String) : List[String] = { //TODO move to an analyzer
        val alternatives = new java.util.HashSet[String]()
        alternatives.add(sf)
        alternatives.add(sf.toLowerCase)
        if (sf.toLowerCase.startsWith("the ")) {
            alternatives.add(sf.substring(3).trim())
            alternatives.add(sf.toLowerCase.replace("the ",""))
        }
        if (sf.toLowerCase.startsWith("a ")) {
            alternatives.add(sf.substring(1).trim())
            alternatives.add(sf.toLowerCase.replace("a ",""))
        }
        if (sf.toLowerCase.startsWith("an ")) {
            alternatives.add(sf.substring(2).trim())
            alternatives.add(sf.toLowerCase.replace("an ",""))
        }
        if (sf.toLowerCase.endsWith("s")) {
            alternatives.add(sf.substring(0,sf.length()-1).trim())
        }
        //alternatives.add(sf.replaceAll("[^A-Za-z0-9 ]", " ").trim()) // may be problematic with accents
        alternatives.add(sf.replaceAll("[\\^`~!@\\#$%*()_+-={}\\[\\]\\|/\\\\,\\.<>\\?/'\":;]", " ").trim()) //TODO TEST
        alternatives.toList
    }

    // map from URI to list of surface forms
    // used by IndexEnricher
    // uri -> list(sf1, sf2)
    def loadSurfaceForms(surfaceFormsFileName: String, transform : String => List[String]) = {
        var nWrongLines = 0
        SpotlightLog.info(this.getClass, "Getting surface form map...")
        val reverseMap : java.util.Map[String, java.util.LinkedHashSet[SurfaceForm]] = new java.util.HashMap[String, java.util.LinkedHashSet[SurfaceForm]]()
        val separator = "\t"
        val tsvScanner = new Scanner(new FileInputStream(surfaceFormsFileName), "UTF-8")
        while (tsvScanner.hasNextLine) {
            val line = tsvScanner.nextLine.split(separator)
            try {
                val sfAlternatives = transform(line(0)).map(sf => new SurfaceForm(sf))
                val uri = line(1)
                var sfSet = reverseMap.get(uri)
                if (sfSet == null) {
                    sfSet = new java.util.LinkedHashSet[SurfaceForm]()
                }
                sfSet.addAll(sfAlternatives)
                reverseMap.put(uri, sfSet)
            } catch {
                case e: ArrayIndexOutOfBoundsException => nWrongLines = nWrongLines + 1;
            }
        }
        SpotlightLog.info(this.getClass, "Done.")
        if (nWrongLines>0) SpotlightLog.error(this.getClass, "There were %d errors parsing the input lines. Please double check that everything went fine by inspecting the input file given to this class.", nWrongLines)
        reverseMap

    }

    def main(args : Array[String]) {

        val indexingConfigFileName = args(0)
        val sourceIndexFileName = args(1)

        val lowerCased : Boolean = if (args.size>1) args(1).toLowerCase().contains("lowercase") else false
        val alternatives = if (args.size>1) args(1).toLowerCase().contains("alternative") else false

        println("alternatives is %s".format(alternatives.toString))

        val config = new IndexingConfiguration(indexingConfigFileName)
        val targetIndexFileName = sourceIndexFileName+"-withSF"
        val surfaceFormsFileName = config.get("org.dbpedia.spotlight.data.surfaceForms")

        val sfIndexer = new IndexEnricher(sourceIndexFileName,targetIndexFileName, config)

        //val sfMap = loadSurfaceForms(surfaceFormsFileName, if (alternatives) fromTitlesToAlternatives(_) else toLowercase(_,lowerCased))
        val sfMap = loadSurfaceForms(surfaceFormsFileName, fromTitlesToAlternatives)
        sfIndexer.enrichWithSurfaceForms(sfMap)
        sfIndexer.close
    }

}