package org.dbpedia.spotlight.spot.lingpipe

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

import org.apache.commons.logging.LogFactory
import com.aliasi.util.AbstractExternalizable
import java.io.{FileInputStream, File}
import org.semanticweb.yars.nx.parser.NxParser
import com.aliasi.dict.{DictionaryEntry, MapDictionary}
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import org.dbpedia.spotlight.io.IndexedOccurrencesSource
import java.util.ArrayList
import org.dbpedia.spotlight.util.IndexingConfiguration
import io.Source

/**
 * Index surface forms to a spotter dictionary.
 * - from TSV: surface forms must be in the first column.
 * - from NT: surface forms must be literals of URIs.
 * - from list
 *
 * Command line usage:
 *
 *   mvn scala:run org.dbpedia.spotlight.spot.lingpipe.IndexLingPipeSpotter
 * @author maxjakob
 */
object IndexLingPipeSpotter
{
    private val LOG = LogFactory.getLog(this.getClass)


    def getDictionary(occs : List[DBpediaResourceOccurrence], lowerCased: Boolean) : MapDictionary[String] = {
        if (lowerCased) LOG.warn("Lowercasing all surface forms in this dictionary!")
        val dictionary = new MapDictionary[String]()
        for (occ <- occs) {
            val sf = if (lowerCased) occ.surfaceForm.name.toLowerCase else occ.surfaceForm.name
            dictionary.addEntry(new DictionaryEntry[String](sf, ""))  // chunk type undefined
        }
        dictionary
    }

    def getDictionary(surrogatesFile : File) : MapDictionary[String] = {
        LOG.info("Reading surface forms from "+surrogatesFile+"...")
        if (surrogatesFile.getName.toLowerCase.endsWith(".tsv")) getDictionaryFromTSVSurrogates(surrogatesFile)
        else if (surrogatesFile.getName.toLowerCase.endsWith(".nt")) getDictionaryFromNTSurrogates(surrogatesFile)
        else getDictionaryFromList(surrogatesFile)
    }

    def writeDictionaryFile(dictionary : MapDictionary[String], targetFile : File) {
        LOG.info("Saving compiled dictionary to "+targetFile.getName+"...")
        AbstractExternalizable.compileTo(dictionary, targetFile)
        LOG.info(dictionary.size+" entries saved.")
    }

    private def getDictionaryFromNTSurrogates(surrogatesNTFile : File) : MapDictionary[String] = {
        val dictionary = new MapDictionary[String]()
        val nxParser = new NxParser(new FileInputStream(surrogatesNTFile), false)
        while (nxParser.hasNext) {
            val triple = nxParser.next
            val surfaceForm = triple(2).toString
            dictionary.addEntry(new DictionaryEntry[String](surfaceForm, ""))  // chunk type undefined
        }
        dictionary
    }

    private def getDictionaryFromTSVSurrogates(surrogatesTSVFile : File) : MapDictionary[String] = {
        val dictionary = new MapDictionary[String]()
        for (line <- Source.fromFile(surrogatesTSVFile, "UTF-8").getLines) {
            val surfaceForm = line.split("\t")(0)
            dictionary.addEntry(new DictionaryEntry[String](surfaceForm, ""))  // chunk type undefined
        }
        dictionary
    }

    private def getDictionaryFromList(surrogatesListFile : File) : MapDictionary[String] = {
        val dictionary = new MapDictionary[String]()
        for (line <- Source.fromFile(surrogatesListFile, "UTF-8").getLines) {
            val surfaceForm = line.trim
            dictionary.addEntry(new DictionaryEntry[String](surfaceForm, ""))  // chunk type undefined
        }
        dictionary
    }

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)
        var lowerCased = if (args.size>1) args(1).toLowerCase().contains("lowercase") else false
        val source = if (args.size>2) args(2).toLowerCase() else "tsv" // index or tsv

        val config = new IndexingConfiguration(indexingConfigFileName)
        val candidateMapFile = new File(config.get("org.dbpedia.spotlight.data.surfaceForms"))
        val indexDir = new File(config.get("org.dbpedia.spotlight.index.dir"))

        val dictFile = new File(candidateMapFile.getAbsoluteFile+".spotterDictionary")

        val dictionary = if (source=="index")
                                getDictionary(IndexedOccurrencesSource.fromFile(indexDir).foldLeft(List[DBpediaResourceOccurrence]())( (a,b) => b :: a ), lowerCased );
                            else
                                getDictionary(candidateMapFile)
        writeDictionaryFile(dictionary, dictFile)
    }
}