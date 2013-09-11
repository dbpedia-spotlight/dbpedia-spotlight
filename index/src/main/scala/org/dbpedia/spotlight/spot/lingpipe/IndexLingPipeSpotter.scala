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

package org.dbpedia.spotlight.spot.lingpipe

import org.dbpedia.spotlight.log.SpotlightLog
import com.aliasi.util.AbstractExternalizable
import java.io.{FileInputStream, File}
import org.semanticweb.yars.nx.parser.NxParser
import com.aliasi.dict.{DictionaryEntry, MapDictionary}
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import org.dbpedia.spotlight.util.IndexingConfiguration
import io.Source
import org.dbpedia.spotlight.io.{OccurrenceSource, IndexedOccurrencesSource}

/**
 * Index surface forms to a spotter dictionary.
 * - from TSV: surface forms must be in the first column.
 * - from NT: surface forms must be literals of URIs.
 * - from list
 * - from index
 *
 * Command line usage:
 *
 *   mvn scala:run org.dbpedia.spotlight.spot.lingpipe.IndexLingPipeSpotter \
 *                 conf/indexing.properties lowercase [index|tsv]
 *
 * @author maxjakob
 */
object IndexLingPipeSpotter
{
    def getDictionary(occs : Traversable[DBpediaResourceOccurrence], lowerCased: Boolean, uriCountThreshold: Int = 0) : MapDictionary[String] = {
        if (lowerCased) SpotlightLog.warn(this.getClass, "Lowercasing all surface forms in this dictionary!")
        val dictionary = new MapDictionary[String]()
        occs.foreach( occ => {
            val sf = if (lowerCased) occ.surfaceForm.name.toLowerCase else occ.surfaceForm.name
            if (occ.resource.support>uriCountThreshold)
                dictionary.addEntry(new DictionaryEntry[String](sf, ""))  // chunk type undefined
        })
        dictionary
    }

    def getDictionary(surrogatesFile : File, uriCountThreshold: Int) : MapDictionary[String] = {
        SpotlightLog.info(this.getClass, "Reading surface forms from %s...", surrogatesFile)
        if (surrogatesFile.getName.toLowerCase.endsWith(".tsv")) getDictionaryFromTSV(surrogatesFile)
        else if (surrogatesFile.getName.toLowerCase.endsWith(".count")) getDictionaryFromTSVSurrogates(surrogatesFile, uriCountThreshold)
        else if (surrogatesFile.getName.toLowerCase.endsWith(".nt")) getDictionaryFromNTSurrogates(surrogatesFile)
        else getDictionaryFromList(surrogatesFile)
    }

    def writeDictionaryFile(dictionary : MapDictionary[String], targetFile : File) {
        SpotlightLog.info(this.getClass, "Saving compiled dictionary to %s...", targetFile.getName)
        AbstractExternalizable.compileTo(dictionary, targetFile)
        SpotlightLog.info(this.getClass, "%d entries saved.", dictionary.size)
    }

    //TODO enable filtering by count
    private def getDictionaryFromNTSurrogates(surrogatesNTFile : File) : MapDictionary[String] = {
        val dictionary = new MapDictionary[String]()
        val nxParser = new NxParser(new FileInputStream(surrogatesNTFile), false)
        while (nxParser.hasNext) { //TODO this needs more work. should filter by property name
            val triple = nxParser.next
            val surfaceForm = triple(2).toString
            dictionary.addEntry(new DictionaryEntry[String](surfaceForm, ""))  // chunk type undefined
        }
        dictionary
    }

    private def getDictionaryFromTSVSurrogates(surrogatesTSVFile : File, uriCountThreshold: Int) : MapDictionary[String] = {
        val dictionary = new MapDictionary[String]()
        for (line <- Source.fromFile(surrogatesTSVFile, "UTF-8").getLines) {
            val fields = line.split("\t")
            var uriCount = 0
            try {
                uriCount = fields(2).toInt
            } catch {
                case e: Exception => SpotlightLog.error(this.getClass, "Expected input is a TSV file with <surfaceForm, uri, count>")
            }
            val surfaceForm = fields(0)
            dictionary.addEntry(new DictionaryEntry[String](surfaceForm, ""))  // chunk type undefined
        }
        dictionary
    }

    private def getDictionaryFromTSV(surrogatesTSVFile : File) : MapDictionary[String] = {
        val dictionary = new MapDictionary[String]()
        for (line <- Source.fromFile(surrogatesTSVFile, "UTF-8").getLines) {
            val fields = line.split("\t")
            val surfaceForm = fields(0)
            dictionary.addEntry(new DictionaryEntry[String](surfaceForm, ""))  // chunk type undefined
        }
        dictionary
    }

    //TODO how to deal with uriCountThreshold?
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
        val uriCountThreshold = if (args.size>3) args(3).toInt else 0

        val config = new IndexingConfiguration(indexingConfigFileName)
        val candidateMapFile = new File(config.get("org.dbpedia.spotlight.data.surfaceForms"))
        val indexDir = new File(config.get("org.dbpedia.spotlight.index.dir"))

        if (indexDir.getName.contains("compact"))
            SpotlightLog.warn(this.getClass, "Beware, this class cannot operate on compact indexes. Based on the file name, we believe that your index has been run through CompactIndex, therefore removing surface forms from the stored fields.")

        val dictFile = new File(candidateMapFile.getAbsoluteFile+".spotterDictionary")

        val dictionary = if (source=="index")
                                getDictionary(IndexedOccurrencesSource.fromFile(indexDir), lowerCased, uriCountThreshold );
                            else
                                getDictionary(candidateMapFile, uriCountThreshold)
        writeDictionaryFile(dictionary, dictFile)
    }
}
