/**
 * Copyright 2011 DBpedia Spotlight Development Team
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

package org.dbpedia.spotlight.web.rest

import scala.collection.JavaConversions._
import scala.xml.{Node,PrettyPrinter}
import java.net.{URLEncoder,InetAddress}
import org.apache.commons.codec.digest.DigestUtils

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

/**
 * Class that handles the conversion of DBpedia Spotlight annotations
 * to the NLP Interchange Format (NIF).
 * 
 * @author Marcus Nitzschke
 */
class NIFOutputFormatter(options: java.util.HashMap[String,_]){
  // default options for nif processing
//     var options = Map(
//       "format" -> "rdfxml",
//       "prefix" -> "http://example.com/placeholder#",
//       "urirecipe" -> "offset",
//       "context-length" -> 10,
//       "debug" -> false
//     )
  val prefix = options.getOrElse("prefix", InetAddress.getLocalHost.getHostName + "#")
  val recipe = options.getOrElse("urirecipe", "offset").toString

  /**
   * Method for processing the spotlight annotations to NIF format.
   * 
   * @param text the original input text
   * @param jOccs a list of the resource occurrences of the input text
   * @return the NIF representation of the annotated input text
   */
  def outputNIFFromText(text: String, occs: java.util.List[DBpediaResourceOccurrence]): String = {
    // set of all occurrence URIs collected for the substring properties
    var occUris: Set[String] = Set()

    // sequence holding all nodes of the resource occurrences
    var result = Seq[Node]()

    // rdf type of the processed string
    var rdfType = "http://nlp2rdf.lod2.eu/schema/string/"
    if (recipe == "offset") rdfType += "OffsetBasedString" else rdfType += "ContextHashBasedString"

    // begin of the processing of the resource occurrences
    for (occ <- occs){

      // build URI for this occurrence
      var occUri = ""
      try {
        occUri = buildURI(occ)
      }
      catch {
	case iae : IllegalArgumentException => println(iae.getMessage) // TODO error handling
      }
      // add the URI to the collection of occurrence URIs
      occUris += occUri

      // uri of the appropriate DBpedia resource
      val dbpediaResUri = "http://dbpedia.org/resource/" + occ.resource.uri

      // append the rdf output of this occurrence
      result = result union
	<rdf:Description rdf:about={occUri}>
	  <itsrdf:disambigIdentRef rdf:resource={dbpediaResUri} />
	  <rdf:type rdf:resource={rdfType} />
	</rdf:Description>
    }

    // document specific output
    var docUri = ""
    try {
      docUri = buildDocURI(text)
    }
    catch {
      case iae : IllegalArgumentException => println(iae.getMessage) // TODO error handling
    }

    // build final rdfxml output
    result = <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
		      xmlns:itsrdf="http://www.w3.org/2005/11/its/rdf#"
		      xmlns:str="http://nlp2rdf.lod2.eu/schema/string/">
	       <rdf:Description rdf:about={docUri}>
		 <str:sourceString>{text}</str:sourceString>
                 {for (occUri <- occUris) yield <str:subString rdf:resource={occUri} />}
                 <rdf:type rdf:resource={rdfType} />
                 <rdf:type rdf:resource="http://nlp2rdf.lod2.eu/schema/string/Document"/>
               </rdf:Description>
               {result}
	     </rdf:RDF>
 
    //val prettyPrinter = new PrettyPrinter(80,2)
    //prettyPrinter.format(result)
    result toString
  }

  /**
   * Method that builds the URIs for the different occurrences according to
   * the NIF specification.
   * 
   * @param occ DBpediaResourceOccurrence for which the URI should be build
   * @return URI for the specific DBpediaResourceOccurrence
   */
  def buildURI(occ: DBpediaResourceOccurrence): String = {
    if (recipe == "offset") {
      // calculate offset indices according to NIF2.0 spec
      val startInd = occ.textOffset
      val endInd = startInd + occ.surfaceForm.name.length
      prefix + "offset_%s_%s".format(startInd, endInd)
    }
    else if (recipe == "context-hash") {
      val ctxLength = options.getOrElse("context-length", 10)
      val length = occ.surfaceForm.name.length
      val ctxString = "" // TODO
      val hash = DigestUtils.md5Hex(ctxString)
      var string = if (length < 20) occ.surfaceForm.name else occ.surfaceForm.name.substring(0,20)
      string = URLEncoder.encode(string , "UTF-8")
      prefix + "hash_%s_%s_%s_%s".format(ctxLength, length, hash, string)
    }
    else throw new IllegalArgumentException("Wrong uri scheme type")
  }

  /**
   * Method that builds the URI for the whole document according to
   * the NIF specification.
   * 
   * @param text Input string/document for which the URI should be build
   * @return URI for the specific string/document
   */
  def buildDocURI(text: String): String = {
    if (recipe == "offset")
      prefix + "offset_0_%s".format(text.length)
    else if (recipe == "context-hash") {
      val ctxLength = options.getOrElse("context-length", 10)
      val length = text.length
      val ctxString = "" // TODO
      val hash = DigestUtils.md5Hex(ctxString)
      var string = if (length < 20) text else text.substring(0,20)
      string = URLEncoder.encode(string , "UTF-8")
      prefix + "hash_%s_%s_%s_%s".format(ctxLength, length, hash, string)
    }
    else throw new IllegalArgumentException("Wrong uri scheme type")
  }
}
