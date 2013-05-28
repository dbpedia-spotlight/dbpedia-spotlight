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

package org.dbpedia.spotlight.model

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

import org.apache.commons.lang.NotImplementedException
import scalaj.collection.Imports._
/**
 * Class to give a perspective of text items (e.g. paragraphs) containing surface form occurrences to be annotated,
 * instead of the perspective of occurrences that hold a paragraph.
 *
 * @see{AnnotatedParagraph}
 *
 * @author maxjakob
 * @author pablomendes
 */
class Paragraph(val id : String,
                val text : Text,
                val occurrences : List[SurfaceFormOccurrence]) {

    def this(text : Text, occurrences : List[SurfaceFormOccurrence]) =  {
        this("", text, occurrences) // allow empty ids
    }

    def equals(that : Paragraph) : Boolean = {
        throw new NotImplementedException("no equality for paragraphs defined yet") //TODO test for text only or text and occs?
    }

    override def toString = {
        val textLen = text.text.length
        if (!id.isEmpty) id+": " else "" + "Text[" + text.text.substring(0, scala.math.min(textLen, 50)) + " ...]" + occurrences.mkString("\n")
    }

    def getOccurrences() : java.util.List[SurfaceFormOccurrence] = {
        occurrences.asJava
    }
}