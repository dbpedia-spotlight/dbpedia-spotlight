/**
 * Adapted from core/org.dbpedia.spotlight.util.CreateLexicalizations (Copyright 2011 Pablo Mendes, Max Jakob)
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
import org.dbpedia.extraction.util.WikiUtil

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 03.02.11
 * Time: 16:06
 * Creates the dataset of lexicalizations including scores of pointwise mutual information.
 */

object CreateLookupIndex {

    val MIN_PAIR_COUNT = 3


//    val scoresNamedGraph = "<http://dbepdia.org/spotlight/score>"
//
//    val namedGraphPrefix = "<http://dbepdia.org/spotlight/id/"
//
//    val pmiScoreRelation            = "<http://dbpedia.org/spotlight/score#pmi>"
//    val sfGivenUriScoreRelation     = "<http://dbpedia.org/spotlight/score#sfGivenUri>"
//    val uriGivenSfScoreRelation     = "<http://dbpedia.org/spotlight/score#uriGivenSf>"
//    val uriProbabilityScoreRelation = "<http://dbpedia.org/spotlight/score#uriProbability>"
      val uriCountScoreRelation       =
        "http://dbpedia.org/property/refCount";
        //"<http://dbpedia.org/spotlight/score#uriCount>"

    //val resourcePrefix = "<http://fr.dbpedia.org/resource/"
    val lexvoLabelRelation = "http://lexvo.org/ontology#label"


    private def getDataType(value: Any, langCode: String ="en"): Option[String] = value match {
        case v: Double => Some("^^<http://www.w3.org/2001/XMLSchema#double>")
        case v: Int => Some("^^<http://www.w3.org/2001/XMLSchema#integer>")
        case v: String => Some("@" + langCode)
        case _ => None
    }



    def main(args: Array[String]) {
        val indexingConfigFileName = args(0)
        val config = new IndexingConfiguration(indexingConfigFileName)

        val languageCode = config.get("org.dbpedia.spotlight.language_i18n_code","en")
        //println("language : "+ languageCode)
        val resourcePrefix = config.get("org.dbpedia.spotlight.default_namespace","http://fr.dbpedia.org/resource/")
        
        val surrogateFileCountPath = config.get("org.dbpedia.spotlight.data.surrogateFile")
        val surrogateCountsFile = new File(surrogateFileCountPath)
        
        val ouputFilePath = config.get("org.dbpedia.spotlight.data.lookupInputFile")
        val outFile = new File(ouputFilePath)
                
//        val outFile = new File(args(0))
//        val surrogateCountsFile = new File(args(1))

        val surrogates = getLinkedSurrogatesMap(surrogateCountsFile)
        val ntOut = new PrintStream(outFile)
        val readableOut = new PrintStream(outFile+".read")

        writeAll(surrogates, ntOut, readableOut, languageCode, resourcePrefix)

        ntOut.close
        readableOut.close
    }


    def getSurrogatesMap(surrogatesFile: File) : Map[(String,String),Int] = {
        var m = Map[(String,String),Int]()
        Source.fromFile(surrogatesFile, "UTF-8").getLines.foreach{ line =>
            val el = line.trim.split("\\s+", 4)
            val count = el(0).toInt
            val uri = el(2)
            val sf = el(1)

            m = m.updated((uri, sf), m.get((uri, sf)).getOrElse(0) + count.toInt)
        }
        m
    }

    /**
     * (URI -> (SF -> count))
     */
    def getLinkedSurrogatesMap(surrogatesFile: File) : Map[String,Map[String,Int]] = {
        System.err.println("Reading surrogates from "+surrogatesFile+" ...")
        var m = Map[String,Map[String,Int]]()
        Source.fromFile(surrogatesFile, "UTF-8").getLines.foreach{ line =>
//            val el = line.trim.split("\\s+", 3)
//
//            if(el.length == 3) {  //  && el(0).toInt >= MIN_PAIR_COUNT
//                val count = el(0).toInt
//                val uri = el(2)
//                val sf = el(1)
              val el = line.trim.split("\\s+")
	          val count = el(0).toInt
	          
	          if(count >= MIN_PAIR_COUNT){
		            
		        val uri = el(el.length-1)
		        val sf = el.slice(1,el.length-1).mkString(" ")
                
                //println("found " + sf + " -> " + resourcePrefix+uri + " x"+count)

                var sfMap = m.get(uri).getOrElse(Map[String,Int]())
                val updatedCount = sfMap.get(sf).getOrElse(0) + count.toInt
                sfMap = sfMap.updated(sf, updatedCount)

                m = m.updated(uri, sfMap)
            }
        }
        System.err.println("Done.")
        m
    }

    def writeAll(
        surrogates: Map[String,Map[String,Int]], 
        out: PrintStream, 
        readableOut: PrintStream, 
        langCode: String, 
        resourcePrefix : String)
    {
      
        var totalOccCount: Double = 0.0
        var globalSfMap = Map[String,Int]()

        System.err.println("Getting total occurrence count, making global surface form map...")
        // get totalOccCount; fill global surface form map
        for((uri,sfMap) <- surrogates) {
            for((sf,pairCount) <- sfMap) {
                globalSfMap = globalSfMap.updated(sf, globalSfMap.get(sf).getOrElse(0) + pairCount)
                totalOccCount += pairCount
            }
        }
        System.err.println("Done.")

        System.err.println("Writing data...")
        for((uri,sfMap) <- surrogates) {
          val fullURI = resourcePrefix + uri
            val uriCount = sfMap.values.sum
            //writeCount
            writeLitteralTriple(out, fullURI, uriCount, uriCountScoreRelation, langCode)
            writeReadable(readableOut, fullURI, uriCount, uriCountScoreRelation)

            for((sf, pairCount) <- sfMap if pairCount >= MIN_PAIR_COUNT) {
            	writeLitteralTriple(out, fullURI, sf, lexvoLabelRelation, langCode)
            }
        }
        System.err.println("Done.")
    }

    
    private def writeLitteralTriple(out : PrintStream, uri : String, value : Any, property : String, langCode : String) {
        val sb = new StringBuilder()
        sb append "<" append uri append "> "
        sb append "<" append property append "> "
        sb append "\"" append value append "\"" append getDataType(value, langCode).get
        sb append " ."

        out.println(sb.toString)
    }

//    private def writeTriple(out : PrintStream, uri : String, score : AnyVal, scoreRel : String) {
//        val sb = new StringBuilder()
//        sb append resourcePrefix append uri append "> "
//        sb append " "
//        sb append scoreRel append " \"" append score append "\"" append getDataType(score).get
//        sb append " ."
//
//        out.println(sb.toString)
//    }
//    
//
//    private def writeQuad(out : PrintStream, uri : String, sf : String, score : Double, scoreRel : String) {
//        val sb = new StringBuilder()
//        sb append resourcePrefix append uri append "> "
//        sb append lexvoLabelRelation
//        sb append " "
//        sb append '"'
//        escapeString(sb, sf)
//        sb append '"'
//        sb append "@en "
//        sb append namedGraphPrefix append uri append "---" append WikiUtil.wikiEncode(sf) append ">"
//        sb append " ."
//        sb append "\n"
//
//        sb append namedGraphPrefix append uri append "---" append WikiUtil.wikiEncode(sf) append ">"
//        sb append " "
//        sb append scoreRel append " \"" append score append "\"" append getDataType(score).get
//        sb append " "
//        sb append scoresNamedGraph
//        sb append " ."
//
//        out.println(sb.toString)
//    }

    private def writeReadable(out : PrintStream, uri : String, score : AnyVal, scoreRel : String) {
        out.println(uri+"\t"+score+"\t"+scoreRel)
    }

    private def writeReadable(out : PrintStream, uri : String, sf : String, score : Double, scoreRel : String) {
        out.println(uri+"\t"+score+"\t"+sf+"\t"+scoreRel)
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