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

import org.dbpedia.spotlight.model.{SpotlightConfiguration, SurfaceForm, Text, SurfaceFormOccurrence}
import scala.collection.JavaConversions._
import com.aliasi.util.AbstractExternalizable
import org.dbpedia.spotlight.log.SpotlightLog
import com.aliasi.dict.{Dictionary, ExactDictionaryChunker}
import java.io.File
import org.dbpedia.spotlight.spot.{JAnnotationTokenizerFactory, Spotter}
import org.apache.lucene.analysis.br.BrazilianAnalyzer
import javax.sound.sampled.LineUnavailableException
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.Analyzer
import org.dbpedia.spotlight.lucene.LuceneManager.DBpediaResourceField
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory

/**
 * Spotter using LingPipe (http://alias-i.com/lingpipe/demos/tutorial/ne/read-me.html)
 *
 * To initialize the spotter for a dictionary with
 *   - 3.35 mio entries (typed resources, titles, redirects, disambiguations, occurrences)
 *     you need at least 4G of Java heap space (3500M is too little)
 *   - 2.77 mio entries (typed resources, titles, redirects, disambiguations)
 *     you need at least 3500M of Java heap space (3G is too little)
 *
 * @author maxjakob
 **/

class LingPipeSpotter(val dictionary : Dictionary[String], analyzer:Analyzer, val overlap : Boolean=false, val caseSensitive : Boolean=false)
        extends Spotter
{
    var fileName = "Dictionary[String]";

    var name = ""

    SpotlightLog.debug(this.getClass, "Allow overlap: %s", overlap)
    SpotlightLog.debug(this.getClass, "Case sensitive: %s", caseSensitive)

    def this(dictionaryFile : File, analyzer:Analyzer, overlap : Boolean, caseSensitive : Boolean) = {
        this(AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]],analyzer, overlap, caseSensitive)
        fileName = dictionaryFile.getAbsolutePath
        SpotlightLog.debug(this.getClass, "Dictionary: %s", dictionaryFile)
    }

    def this(dictionaryFile : File, analyzer:Analyzer) = {
        this(AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]],analyzer)
        fileName = dictionaryFile.getAbsolutePath
        SpotlightLog.debug(this.getClass, "Dictionary: %s", dictionaryFile)
    }

    SpotlightLog.info(this.getClass, "Initiating LingPipeSpotter ... (%s)", fileName)
    val dictionaryChunker = new ExactDictionaryChunker(dictionary,
                                                       IndoEuropeanTokenizerFactory.INSTANCE,  // splits "don't" into "don", "'" and "t"
                                                       // AnnotationTokenizerFactory, //English only
                                                       //new JAnnotationTokenizerFactory(analyzer),
                                                       overlap,        // find all matches, including overlapping ones?
                                                       caseSensitive)  // case-sensitive matching?
    SpotlightLog.info(this.getClass, "Done.")

    /**
     * Extracts a set of surface form occurrences from text.
     */
    def extract(text : Text) : java.util.List[SurfaceFormOccurrence] = {
        SpotlightLog.debug(this.getClass, "Spotting with dictionary: %s.", fileName)
        val chunkSet = dictionaryChunker.chunk(text.text).chunkSet
        chunkSet.toList.map{ chunk =>
            val textOffsetStart = chunk.start
            val textOffsetEnd = chunk.end
            //val chunkType = chunk.`type`   // empty for ExactDictionaryChunker
            //val score = chunk.score      // not interesting for us
            val surfaceForm = new SurfaceForm( text.text.substring(textOffsetStart, textOffsetEnd) )
            new SurfaceFormOccurrence(surfaceForm, text, textOffsetStart)
        }.sortBy(_.textOffset)
    }

    /**
     * Every spotter has a name that describes its strategy
     * (for comparing multiple spotters during evaluation)
     */
    def getName() : String = {
        if (name=="") {
            val allMatches = if (dictionaryChunker.returnAllMatches) "overlapping" else "non-overlapping"
            val caseSensitivity = if (dictionaryChunker.caseSensitive) "case-sensitive" else "case-insensitive"
            "LingPipeExactSpotter["+allMatches+","+caseSensitivity+"]"
        } else {
            name
        }

    }

    def setName(newName: String) {
        name = newName;
    }
    
}