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


class DBpediaResourceOccurrence(val id : String,
                                val resource : DBpediaResource,
                                val surfaceForm : SurfaceForm,
                                val context : Text,
                                val textOffset : Int,
                                val provenance : Provenance.Value = Provenance.Undefined,
                                var similarityScore : Double = -1,
                                var percentageOfSecondRank : Double = -1,
                                var contextualScore: Double = -1)
        extends HasFeatures with Comparable[DBpediaResourceOccurrence] {

    setFeature(new Score("finalScore", similarityScore))
    setFeature(new Score("contextualScore", contextualScore))
    setFeature(new Score("percentageOfSecondRank", percentageOfSecondRank))

    //TODO there are a lot of constructors here, because Scala keyword arguments do not mix well with Java; cleaning up anyone?

    def this(resource : DBpediaResource, surfaceForm : SurfaceForm, context : Text,  textOffset : Int, provenance : Provenance.Value, similarityScore : Double) = {
        this("", resource, surfaceForm, context, textOffset, provenance, similarityScore)
    }

    def this(resource : DBpediaResource, surfaceForm : SurfaceForm, context : Text,  textOffset : Int, provenance : Provenance.Value) = {
        this("", resource, surfaceForm, context, textOffset, provenance)
    }

    def this(resource : DBpediaResource, surfaceForm : SurfaceForm, context : Text,  textOffset : Int, similarityScore : Double) = {
        this("", resource, surfaceForm, context, textOffset, Provenance.Undefined, similarityScore)
    }

    def this(resource : DBpediaResource, surfaceForm : SurfaceForm, context : Text,  textOffset : Int) = {
        this("", resource, surfaceForm, context, textOffset)
    }

    def compareTo(that : DBpediaResourceOccurrence) : Int = {
        val c = this.similarityScore.compare(that.similarityScore)
        val str1 : String = this.id+this.resource.uri+this.surfaceForm.name+this.textOffset.toString+this.context.text
        val str2 : String = that.id+that.resource.uri+that.surfaceForm.name+that.textOffset.toString+that.context.text
        if (c==0) str1.compare(str2) else c
    }


    override def equals(obj : Any) : Boolean = {
        obj match {
            case that: DBpediaResourceOccurrence =>
                (  resource.equals(that.resource)
                && surfaceForm.equals(that.surfaceForm)
                && context.equals(that.context)
                //&& (textOffset == that.textOffset )
                )
            case _ => false;
        }
    }

    override def toString = {
        val span = 50
        val start = if (textOffset<0 || textOffset < span) 0 else textOffset-span
        val end = if (textOffset+span > context.text.length) context.text.length else textOffset+span
        //System.err.printf("textOffset: %s, start:%s, end:%s, context.text.length:%s, similarityScore:%s,", textOffset.toString, start.toString, end.toString, context.text.length.toString, similarityScore.toString);
        val text = if (start>end) "Text[]" else "Text[... " + context.text.substring(start, end) + " ...]"    
        val score = if (similarityScore == -1.0) "" else "%.3f".format(similarityScore)
        //surfaceForm + " - at position *" + textOffset + "* in - Text[..." + context.text.substring(start,end) + "...]"
        if (!id.isEmpty) id+": " else "" +surfaceForm+" -"+score+"-> "+resource+" - at position *"+textOffset+"* in - "+text
    }

    override def hashCode = {
      val str = id+resource.uri+surfaceForm.name+textOffset.toString+context.text;
      //if (!id.isEmpty) id.hashCode() else str.hashCode();
      //if (!id.isEmpty) 0 else 1;
      str.hashCode();
    }

    def toTsvString = {
        id+"\t"+resource.uri+"\t"+surfaceForm.name+"\t"+context.text.replaceAll("\\s+", " ")+"\t"+textOffset+"\t"+resource.types.map(_.typeID).mkString(",")
    }


    def setSimilarityScore(s: Double) {
        setFeature(new Score("finalScore", similarityScore))
        this.similarityScore = s
    }

    def setPercentageOfSecondRank(p: Double) {
        setFeature(new Score("percentageOfSecondRank", percentageOfSecondRank))
        this.percentageOfSecondRank = p
    }

    def setContextualScore(p: Double) {
      setFeature(new Score("contextualScore", p))
      this.contextualScore = p
    }


}

