/*
 *
 *   Copyright 2011 Pablo Mendes, Max Jakob
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.dbpedia.spotlight.model

import org.apache.commons.lang.NotImplementedException

/**
 * Class to give a perspective of text items (e.g. paragraphs) containing occurrences,
 * instead of the perspective of occurrences that hold a paragraph.
 *
 * @author maxjakob
 * @author pablomendes
 */
class AnnotatedParagraph(val id : String,
                val text : Text,
                val occurrences : List[DBpediaResourceOccurrence]) {

    def this(text : Text, occurrences : List[DBpediaResourceOccurrence]) =  {
        this("", text, occurrences) // allow empty ids
    }

    def equals(that : AnnotatedParagraph) : Boolean = {
        throw new NotImplementedException("no equality for paragraphs defined yet") //TODO test for text only or text and occs?
    }

    override def toString = {
        val textLen = text.text.length
        val apId = if (!id.isEmpty) id+": " else ""
        apId+ //"[Text " + text.text.substring(0, scala.math.min(textLen, 50)) + " ..]" +
            "<text>\n" + text.text + "\n</text>" +
            "\n<occurrences>\n"+occurrences.map(o => "'%s'@%d => [%s].".format(o.surfaceForm.name,o.textOffset,o.resource.uri)).mkString("\n")+
            "\n</occurrences>\n"
    }

}