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
package org.dbpedia.spotlight.evaluation.external

import org.apache.commons.httpclient._
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.exceptions.AnnotationException
import org.dbpedia.spotlight.model._
import java.io.File
import org.apache.commons.httpclient.methods.GetMethod
import net.sf.json.JSONObject
import org.apache.commons.beanutils.PropertyUtils
import java.util
import java.lang.reflect.InvocationTargetException
import scala.collection.JavaConversions._

/**
 * Simple client to the Open Calais REST API to extract DBpediaResourceOccurrences.
 * This is by no means a complete client for OpenCalais. If that's what you're looking for, try http://code.google.com/p/j-calais/
 * Our client aims at simply returning DBpediaResourceOccurrences for evaluation.
 *
 * Author: Pablo Mendes (original java version) and Alexandre Cançado Cardoso (scala translation)
 * Created: 08/13
 * Last Modified: 26th/08/13
 */


class OpenCalaisClientScala(api_key: String)  extends AnnotationClientScala {

  override val LOG: Log = LogFactory.getLog(this.getClass)

  private val url: String = "http://api.opencalais.com/tag/rs/enrich"
  //Create an instance of HttpClient.
  var client: HttpClient = new HttpClient

  var id: String = "id"
  var submitter: String = "dbpa"
  var outputFormat: String = "application/json"
  var paramsXml: String = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\"\n" +
    "              xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
    "      <c:processingDirectives\n" + "        c:contentType=\"TEXT/RAW\"\n" +
    "        c:outputFormat=\"" + outputFormat + "\"\n" +
    "        c:calculateRelevanceScore=\"true\"\n" +
    "        c:enableMetadataType=\"SocialTags\"\n" +
    "        c:docRDFaccessible=\"false\"\n" +
    "        c:omitOutputtingOriginalText=\"true\"\n" +
    "        ></c:processingDirectives>\n" +
    "      <c:userDirectives\n" +
    "        c:allowDistribution=\"false\"\n" +
    "        c:allowSearch=\"false\"\n" +
    "        c:externalID=\"" + id + "\"\n" +
    "        c:submitter=\"" + submitter + "\"\n" +
    "        ></c:userDirectives>\n" +
    "      <c:externalMetadata></c:externalMetadata>\n" +
    "    </c:params>"

  private def dereference(uri: String): String = {
    LOG.debug("Dereferencing: " + uri)
    val method: GetMethod = new GetMethod(uri)
    method.setRequestHeader("Accept", "rdf/xml")
    val response: String = request(method)

    val hackSameAs: Array[String] = response.split("<owl:sameAs rdf:resource=\"http://dbpedia.org/resource/")
    if (hackSameAs.length > 1) {
      val dbpediaURI: String = hackSameAs(1).substring(0, hackSameAs(1).indexOf("\"/>"))
      if (dbpediaURI.length > 1) // "http://"
        return dbpediaURI
    }

    val hackRedirection: Array[String] = response.split("<cld:redirection rdf:resource=\"")
    if (hackRedirection.length > 1) {
      val redirect: String = hackRedirection(1).substring(0, hackRedirection(1).indexOf("\"/>"))
      if (redirect.length > 7) // "http://"
        return dereference(redirect)
    }

    val hackLabel: Array[String] = response.split("<c:name>")
    if (hackLabel.length > 1) {
      val label: String = hackLabel(1).substring(0, hackLabel(1).indexOf("</c:name>"))
      if (label.length > 1) // "http://"
        return label
    }

    LOG.debug("... resulting in: " + uri)

    uri
  }

  private def parseJson(rawText: Text, annotatedText: String): List[DBpediaResource] = {
    var entities: List[DBpediaResource] = List[DBpediaResource]()
    val jsonObj: JSONObject = JSONObject.fromObject(annotatedText)
    val entriesAux: util.Set[_] = jsonObj.entrySet
    val entries = entriesAux.toSet

    print("debug")
    for (o <- entries){
      //val m: Map = o
      val (entryKey, value) = o
      val key : String = entryKey.toString

      if (key != "doc") {
        //val bean: AnyRef = net.sf.json.JSONObject.toBean(m.getValue.asInstanceOf[JSONObject])
        val bean: AnyRef = net.sf.json.JSONObject.toBean(value.asInstanceOf[JSONObject])

        try{
          val entryType: AnyRef = PropertyUtils.getProperty(bean, "_typeGroup")

          if (entryType == "entities") {
            val uri: String = key
            val entryName: AnyRef = PropertyUtils.getProperty(bean, "name")
            val typeProperty: String = PropertyUtils.getProperty(bean, "_type").asInstanceOf[String]
            val relevance: Double = PropertyUtils.getProperty(bean, "relevance").asInstanceOf[Double]

            val instances: List[_] = PropertyUtils.getProperty(bean, "instances").asInstanceOf[List[_]]

            for (i <- instances) {
              val offset: Int = PropertyUtils.getProperty(i, "offset").asInstanceOf[Int]
              val dbpediaUri: String = dereference(uri)
              val resource: DBpediaResource = new DBpediaResource(dbpediaUri)
              entities = entities :+ resource
            }
          }

        } catch{
          case e: IllegalArgumentException => e.printStackTrace()
          case e: InvocationTargetException => e.printStackTrace()
          case e: NoSuchMethodException => {
            e.printStackTrace()
            print("\n")
          }
        }
      }

    }


    entities
  }


  @throws(classOf[AnnotationException])
  def extract(text: Text): List[DBpediaResource] = {
    val entities: List[DBpediaResource] = parseJson(text, process(text.text))

    entities
  }

  protected def process(text: String): String = {
    null
  }

}


object OpenCalaisClientScala{

  def main(args: Array[String]) {
    val apikey: String = args(0)

    val inputFile: File = new File("/home/alexandre/Projects/Test_Files/Germany.txt")
    val outputFile: File = new File("/home/alexandre/Projects/Test_Files/OpenCalais-java_Germany.list")

    try {
      val client: OpenCalaisClient = new OpenCalaisClient(apikey)
      client.evaluate(inputFile, outputFile)
    } catch {
      case e: Exception => e.printStackTrace
    }
  }

}
