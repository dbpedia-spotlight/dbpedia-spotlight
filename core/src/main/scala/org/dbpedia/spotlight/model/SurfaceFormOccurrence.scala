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

package org.dbpedia.spotlight.model

class SurfaceFormOccurrence(val surfaceForm : SurfaceForm,
                            val context : Text,
                            var textOffset : Int,
                            val provenance : Provenance.Value,
                            var spotProb : Double = -1) extends HasFeatures with Ordered[SurfaceFormOccurrence]
{


    def this(surfaceForm : SurfaceForm, context : Text, textOffset : Int) =
    {
        this(surfaceForm, context, textOffset, provenance = Provenance.Undefined)
    }

    def setTextOffset(newTextOffset: Int) {
        textOffset = newTextOffset
    }

    override def equals(that : Any) : Boolean =
    {
        that match {
            case sfo: SurfaceFormOccurrence => {
                (this.surfaceForm.equals(sfo.surfaceForm)
                && this.context.equals(sfo.context)   // have to be careful here because context can be shortened
                && (this.textOffset == sfo.textOffset)
                    )
            }
            case _ => false;
        }

    }

    override def hashCode() = {
        3 * this.surfaceForm.hashCode() + 5 * this.context.hashCode() + 7 * this.textOffset.hashCode()
    }
    
    override def toString = {
        val span = 50
        val start = if (textOffset < span) 0 else textOffset-span
        val end = if (textOffset+span > context.text.length) context.text.length else textOffset+span
        val text = "Text[... " + context.text.substring(start, end) + " ...]"
        surfaceForm+" - at position *"+textOffset+"* in - "+text
    }

    override def compare(that: SurfaceFormOccurrence): Int = this.textOffset.compare(that.textOffset)

    def contains(that: SurfaceFormOccurrence): Boolean = {
      val endThis = this.textOffset+this.surfaceForm.name.length
      val endThat = that.textOffset+that.surfaceForm.name.length

      this.textOffset <= that.textOffset && endThat <= endThis
    }

    def intersects(that: SurfaceFormOccurrence): Boolean = {
      //Borrowed from the OpenNLP Span class:

      val startThis = this.textOffset
      val startThat = that.textOffset
      val endThis = this.textOffset+this.surfaceForm.name.length
      val endThat = that.textOffset+that.surfaceForm.name.length

      //either s's start is in this or this' start is in s
      this.contains(that) || that.contains(this) ||
        startThis <= startThat && startThat < endThis ||
        startThat <= startThis && startThis < endThat
    }

}