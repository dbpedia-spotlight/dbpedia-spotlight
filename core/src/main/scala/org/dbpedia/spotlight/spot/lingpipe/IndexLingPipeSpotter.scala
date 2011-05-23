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

package org.dbpedia.spotlight.spot.lingpipe

import org.apache.commons.logging.LogFactory
import com.aliasi.util.AbstractExternalizable
import io.Source
import java.io.{FileInputStream, File}
import org.semanticweb.yars.nx.parser.NxParser
import com.aliasi.dict.{DictionaryEntry, MapDictionary}
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import org.dbpedia.spotlight.io.IndexedOccurrencesSource
import java.util.ArrayList

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 24.08.2010
 * Time: 15:28:51
 * Index surface forms to a spotter dictionary.
 * - from TSV: surface forms must be in the first column.
 * - from NT: surface forms must be literals of URIs.
 * - from list
 */

object IndexLingPipeSpotter
{
    private val LOG = LogFactory.getLog(this.getClass)


    def getDictionary(occs : List[DBpediaResourceOccurrence]) : MapDictionary[String] = {
        LOG.warn("Lowercasing all surface forms in this dictionary!")
        val dictionary = new MapDictionary[String]()
        for (occ <- occs) {
            dictionary.addEntry(new DictionaryEntry[String](occ.surfaceForm.name.toLowerCase, ""))  // chunk type undefined
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
        val surrogatesFile = new File(args(0))
        val dictFile = if (args.length > 1) new File(args(1)) else new File(surrogatesFile.getAbsolutePath+".spotterDictionary")

        //val dictionary = getDictionary(surrogatesFile)
        val dictionary = getDictionary(IndexedOccurrencesSource.fromFile(surrogatesFile).foldLeft(List[DBpediaResourceOccurrence]())( (a,b) => b :: a ) );
        writeDictionaryFile(dictionary, dictFile)
    }
}