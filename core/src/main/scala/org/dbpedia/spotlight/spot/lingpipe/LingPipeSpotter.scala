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

import org.dbpedia.spotlight.model.{SurfaceForm, Text, SurfaceFormOccurrence}
import scala.collection.JavaConversions._
import com.aliasi.util.AbstractExternalizable
import org.apache.commons.logging.LogFactory
import com.aliasi.dict.{Dictionary, ExactDictionaryChunker}
import java.io.File
import org.dbpedia.spotlight.spot.Spotter

/**
 * User: Max
 * Date: 24.08.2010
 * Time: 11:59:29
 * Spotter using LingPipe (http://alias-i.com/lingpipe/demos/tutorial/ne/read-me.html)
 *
 * To initialize the spotter for a dictionary with
 *   - 3.35 mio entries (typed resources, titles, redirects, disambiguations, occurrences)
 *     you need at least 4G of Java heap space (3500M is too little)
 *   - 2.77 mio entries (typed resources, titles, redirects, disambiguations)
 *     you need at least 3500M of Java heap space (3G is too little)
 */

class LingPipeSpotter(val dictionary : Dictionary[String], val overlap : Boolean=false, val caseSensitive : Boolean=false)
        extends Spotter
{
    private val LOG = LogFactory.getLog(this.getClass)
    var fileName = "Dictionary[String]";

    def this(dictionaryFile : File, overlap : Boolean, caseSensitive : Boolean) = {
        this(AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]], overlap, caseSensitive)
        fileName = dictionaryFile.getAbsolutePath
        LOG.debug("Dictionary: "+dictionaryFile)
    }

    def this(dictionaryFile : File) = {
        this(AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]])
        fileName = dictionaryFile.getAbsolutePath
        LOG.debug("Dictionary: "+dictionaryFile)
    }

    LOG.info("Initializing LingPipeSpotter ... ("+fileName+")")
    val dictionaryChunker = new ExactDictionaryChunker(dictionary,
                                                       //IndoEuropeanTokenizerFactory.INSTANCE,  // splits "don't" into "don", "'" and "t"
                                                       AnnotationTokenizerFactory,
                                                       overlap,        // find all matches, including overlapping ones?
                                                       caseSensitive)  // case-sensitive matching?
    LOG.info("Done.")

    /**
     * Extracts a set of surface form occurrences from text.
     */
    def extract(text : Text) : java.util.List[SurfaceFormOccurrence] = {
        LOG.debug("Spotting with dictionary: %s.".format(fileName))
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
    def name() : String = {
        val allMatches = if (dictionaryChunker.returnAllMatches) "overlapping" else "non-overlapping"
        val caseSensitivity = if (dictionaryChunker.caseSensitive) "case-sensitive" else "case-insensitive"
        "LingPipeExactSpotter["+allMatches+","+caseSensitivity+"]"
    }
    
}