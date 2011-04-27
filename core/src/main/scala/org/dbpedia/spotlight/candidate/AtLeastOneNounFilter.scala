/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dbpedia.spotlight.candidate

import com.aliasi.sentences.IndoEuropeanSentenceModel
import java.io.File
import org.dbpedia.spotlight.tagging.lingpipe.{LingPipeTaggedTokenProvider, LingPipeFactory}
import scalaj.collection.Imports._

import org.dbpedia.spotlight.model.{Provenance, SurfaceForm, Text, SurfaceFormOccurrence}
import org.apache.commons.logging.LogFactory

/**
 *
 * @author pablomendes
 */
class AtLeastOneNounFilter extends SpotSelector {

    private val LOG = LogFactory.getLog(this.getClass)

    LingPipeFactory.setSentenceModel(new IndoEuropeanSentenceModel)
    LingPipeFactory.setTaggerModelFile(new File("/home/pablo/data/brown/pos-en-general-brown.HiddenMarkovModel"))
    val tagger = new LingPipeTaggedTokenProvider

    var lastText = ""
    def select(occurrences: java.util.List[SurfaceFormOccurrence]) : java.util.List[SurfaceFormOccurrence] = {
        val occs = occurrences.asScala
        lastText = occs.head.context.text
        tagger.initialize(lastText);
        occs.filter(o => {
            if (!(lastText equals o.context.text)) {
                LOG.info("resetting text")
                lastText = occs.head.context.text
                tagger.initialize(lastText)
            }
            val tokens = tagger.getTaggedTokens(o.textOffset, o.textOffset + o.surfaceForm.name.length)
            //println(tokens)
            val atLeastOneNoun = (None != tokens.asScala.find( t => t.getPOSTag.startsWith("n") )) // at least one token is a noun.
            //if (!atLeastOneNoun) println("Contains no nouns: "+tokens) //LOG.trace(o.surfaceForm);
            atLeastOneNoun
        }).asJava
    }

}


