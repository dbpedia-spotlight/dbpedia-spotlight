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

    def this(dictionaryFile : File, overlap : Boolean, caseSensitive : Boolean) = {
        this(AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]], overlap, caseSensitive)
        LOG.debug("Dictionary: "+dictionaryFile)
    }

    def this(dictionaryFile : File) = {
        this(AbstractExternalizable.readObject(dictionaryFile).asInstanceOf[Dictionary[String]])
        LOG.debug("Dictionary: "+dictionaryFile)
    }

    LOG.info("Initializing LingPipeSpotter ...")
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
        val chunkSet = dictionaryChunker.chunk(text.text).chunkSet
        chunkSet.toList.map{ chunk =>
            val textOffsetStart = chunk.start
            val textOffsetEnd = chunk.end
            //val chunkType = chunk.type   // not interesting for us
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