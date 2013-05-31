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

package org.dbpedia.spotlight.string

import org.dbpedia.spotlight.exceptions.InputException
import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, SurfaceFormOccurrence, Text}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Contains functions to limit the amount of context for disambiguation.
 */

class ContextExtractor(minContextWords : Int, maxContextWords : Int) {

    private val spaceChars = Set(' ', '\n', '\t')

    /**
     * Strips the context of a DBpediaResourceOccurrence
     */
    def narrowContext(occ : DBpediaResourceOccurrence) : DBpediaResourceOccurrence = {
        val sfOcc = new SurfaceFormOccurrence(occ.surfaceForm, occ.context, occ.textOffset)
        try {
            val newSfOcc = narrowContext(sfOcc)
            new DBpediaResourceOccurrence(occ.id, occ.resource, newSfOcc.surfaceForm, newSfOcc.context, newSfOcc.textOffset)
        } catch {
            case e: Exception => println(occ.context); throw e
        }
    }

    /**
     * Strips the context of a SurfaceFormOccurrence
     */
    def narrowContext(occ : SurfaceFormOccurrence) : SurfaceFormOccurrence = {
        val sb = new StringBuffer(occ.surfaceForm.name)  //StringBuilder is more efficient but not thread-safe

        var l = occ.textOffset - 1                            //left-hand char of surface form
        var r = occ.textOffset + occ.surfaceForm.name.length  //right-hand char of surface form

        var wordCount = 0
        while((wordCount < maxContextWords) && !(isEnd(occ.context.text, l, false) && isEnd(occ.context.text, r, true))) {

            //consume words to the left
            val newL = consume(occ.context.text, l, sb, false)
            if(newL != l) {
                wordCount += 1
            }
            l = newL

            //consume words to the right
            val newR = consume(occ.context.text, r, sb, true)
            if(newR != r) {
                wordCount += 1
            }
            r = newR
        }

        if(wordCount < minContextWords) {
            throw new InputException("not enough context: need at least "+minContextWords+" context words for each spotted surface form") //, found "+wordCount)
        }

        new SurfaceFormOccurrence(occ.surfaceForm, new Text(sb.toString), scala.math.max(0, occ.textOffset-l-1))
    }

    /**
     * Consumes white space and one word after that and adds everything to the StringBuffer.
     * Returns the position that was not consumed yet.
     */
    private def consume(text : String, pos : Int, sb : StringBuffer, readDirection : Boolean) : Int = {
        var newPos = pos
        newPos = consume(text, newPos, sb, readDirection, (s, p) =>  isSpace(s, p))  //consume spaces
        newPos = consume(text, newPos, sb, readDirection, (s, p) => !isSpace(s, p))  //consume letters
        newPos
    }

    private def isSpace(text : String, pos : Int) = spaceChars.contains(text(pos))

    private def consume(text : String, pos : Int, sb : StringBuffer, readDirection : Boolean, charCheck : (String,Int)=>Boolean) : Int = {
        var newPos = pos
        while(!isEnd(text, newPos, readDirection) && charCheck(text, newPos)) {
            addToString(sb, text(newPos), readDirection)
            newPos = updatePosition(newPos, readDirection)
        }
        newPos
    }

    // Returns true if pos is not a valid index of text. The readDirection flag specifies in which direction.
    private def isEnd(text : String, pos : Int, readDirection : Boolean) = if(readDirection) pos >= text.length else pos < 0

    private def addToString(sb : StringBuffer, ch : Char, readDirection : Boolean) = if(readDirection) sb.append(ch) else sb.insert(0, ch)

    private def updatePosition(pos : Int, readDirection : Boolean) = if(readDirection) pos + 1 else pos - 1

}
