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

package org.dbpedia.spotlight.util

import io.Source
import java.io.{PrintStream, File}
import org.dbpedia.spotlight.string.ModifiedWikiUtil

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 03.02.11
 * Time: 16:06
 * Creates the dataset of lexicalizations including scores of pointwise mutual information.
 */

object CreateLexicalizations {

    val MIN_PAIR_COUNT = 20

    val scoreRelation = "<http://dbpedia.org/spotlight/pmiScore>"

    val resourcePrefix = "<http://dbpedia.org/resource/"
    val namedGraphPrefix = "<http://dbepdia.org/spotlight/graph/"

    val scoresNamedGraphPostfix = "surfaceFormScores>"

    val lexvoLabelRelation = "<http://lexvo.org/ontology#label>"
    val xsdDouble = "<http://www.w3.org/2001/XMLSchema#double>"


    var denominator = 0.0  // total number of occurrences


    def getUriMap(uriCountsFile: File) : Map[String,Double] = {
        Source.fromFile(uriCountsFile, "UTF-8").getLines.map{ l =>
            val el = l.split("\\s+", 3)
            denominator += el(1).toDouble
            (el(2), el(1).toDouble)
        }.toMap
    }


    def writeDataset(outFile: File, uriCountsFile: File, sfSortedPairCountsFile: File) {
        println("Making URI counts map...")
        var uriCounts = getUriMap(uriCountsFile)
        println("Done.")

        val out = new PrintStream(outFile, "UTF-8")
        val readableOut = new PrintStream(outFile+".read", "UTF-8")

        var lastSf = ""
        var thisSfCount = 0.0
        var urisForThisSf = Map[String, Double]()

        println("Making dataset...")
        for(line <- Source.fromFile(sfSortedPairCountsFile, "UTF-8").getLines) {
        //for(line <- Source.fromFile(countFile).getLines) {
            val elements = line.split("\\s+", 4)
            // first element is empty
            val count = elements(1).toDouble
            val uri = elements(2)
            val sf = elements(3)

            if(count >= MIN_PAIR_COUNT && sf.trim != "") {          //&& (conceptURIs contains uri)
                if(lastSf != sf && lastSf.trim != "") {

                    val pX = thisSfCount/denominator
                    for((uri, c) <- urisForThisSf) {
                        val pY = uriCounts(uri)/denominator
                        val pXY = c/denominator

                        val pmi = scala.math.log( pXY / (pX * pY) )

                        writeQuad(out, uri, lastSf, pmi, scoreRelation)
                        writeReadable(readableOut, uri, lastSf, pmi)
                    }
                    thisSfCount = 0.0
                    urisForThisSf = Map()
                }
                thisSfCount += count
                urisForThisSf = urisForThisSf.updated(uri, count)
                lastSf = sf
            }

        }
        out.close
        println("Done.")
    }


    def main(args: Array[String]) {
        val outFile = new File(args(0))
        val uriCountsFile = new File(args(1))
        val sfSortedPairCountsFile = new File(args(2))

        writeDataset(outFile, uriCountsFile, sfSortedPairCountsFile)
    }



    private def writeQuad(out : PrintStream, uri : String, sf : String, score : Double, scoreRel : String) {
        val sb = new StringBuilder()
        sb append resourcePrefix append uri append "> "
        sb append lexvoLabelRelation
        sb append " "
        sb append '"'
        escapeString(sb, sf)
        sb append '"'
        sb append "@en "
        sb append namedGraphPrefix append uri append "---" append ModifiedWikiUtil.wikiEncode(sf) append ">"
        sb append " ."
        sb append "\n"

        sb append namedGraphPrefix append uri append "---" append ModifiedWikiUtil.wikiEncode(sf) append ">"
        sb append " "
        sb append scoreRel append " \"" append score append "\"^^" append xsdDouble
        sb append " "
        sb append namedGraphPrefix append scoresNamedGraphPostfix
        sb append " ."
        sb append "\n"

        out.print(sb.toString)
    }

    private def writeReadable(out : PrintStream, uri : String, sf : String, score : Double) {
        out.println(uri+"\t"+score+"\t"+sf)
    }

    // copied from DBpedia Quad.scala
    private def escapeString(sb : StringBuilder, input : String) : StringBuilder =
	{
        // iterate over code points (http://blogs.sun.com/darcy/entry/iterating_over_codepoints)
        val inputLength = input.length
        var offset = 0

        while (offset < inputLength)
        {
            val c = input.codePointAt(offset)
            offset += Character.charCount(c)

    		//Ported from Jena's NTripleWriter
			if (c == '\\' || c == '"')
			{
				sb append '\\' append c.toChar
			}
			else if (c == '\n')
			{
				sb append "\\n"
			}
			else if (c == '\r')
			{
				sb append "\\r";
			}
			else if (c == '\t')
			{
				sb append "\\t"
			}
			else if (c >= 32 && c < 127)
			{
				sb append c.toChar
			}
			else
			{
				val hexStr = c.toHexString.toUpperCase
                val hexStrLen = hexStr.length

                if (c <= 0xffff)
                {
                    // 16-bit code point
                    sb append "\\u"
                    sb append "0" * (4 - hexStrLen)  // leading zeros
                }
                else if (c <= 0x10ffff)  // biggest representable code point
                {
                    // 32-bit code point
                    sb append "\\U"
                    sb append "0" * (8 - hexStrLen)  // leading zeros
                }
                else
                {
                    throw new Exception("code point "+c+" outside of range (0x0000..0x10ffff)")
                }

				sb append hexStr
			}
		}
		return sb
	}


}