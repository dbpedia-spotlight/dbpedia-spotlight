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

import org.dbpedia.spotlight.exceptions.AnnotationException
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.NameValuePair
import org.dbpedia.spotlight.model.{DBpediaResource, Text}
import org.w3c.dom.{NodeList, Element, Node}
import org.dbpedia.spotlight.string.XmlParser
import java.io.{File, IOException}
import org.xml.sax.SAXException
import javax.xml.parsers.ParserConfigurationException
import scala.collection.immutable.List

/**
  * Simple web service-based annotation scala client for Alchemy.
  * Translate from AlchemyClient.java (the java clients were discontinued)
  *
  * Author: Pablo Mendes (original java version). Leandro Bianchini (scala translation) and Alexandre Cançado Cardoso (scala bug fixing)
  * Created: 22th/06/13
  * Last Modified: 23th/08/13
  *
  * Tested for English and Portuguese, ok for both.
  */

class AlchemyClientScala(apikey: String) extends AnnotationClientScala {

  val url: String = "http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities"

  @throws(classOf[AnnotationException])
  def process(text: String): String = {
    val method: PostMethod = new PostMethod(url)
    method setRequestHeader("Content-type","application/x-www-form-urlencoded")

    val params: Array[NameValuePair] = Array(new NameValuePair("text", text), new NameValuePair("apikey", apikey))
    method setRequestBody params

    request(method)
  }


  def extract(text: Text): List[DBpediaResource] = {

    var entities: List[DBpediaResource] = List[DBpediaResource]()
    val response: String = process(text.text)
    var root: Element = null

    try
       root = XmlParser.parse(response)
    catch {
      case e: IOException  => e printStackTrace()
      case e: SAXException => e printStackTrace()
      case e: ParserConfigurationException => e printStackTrace()
    }
    val list: NodeList = XmlParser getNodes("/results/entities/entity", root)

    for(i <- 0 to list.getLength-1){
      val attributes = list.item(i).getChildNodes
      for(j <- 0 to attributes.getLength-1){
        val n = attributes.item(j)
        val name : String = n.getNodeName
        var value : String = ""
        if (n.getNodeType != Node.TEXT_NODE) {
          value = n.getFirstChild.getNodeValue
          LOG.trace(String.format("Name:%s, Value: %s", name, value))
        }
        if (name.equals("text")) {
          entities = entities :+ new DBpediaResource(value)
        }
      }
    }
    LOG.debug(String.format("Extracted: %s", entities))

    entities
  }
}


object AlchemyClientScala {

  def main(args: Array[String]) {
    val apikey: String = args(0)

    val alchemyClient = new AlchemyClientScala(apikey)

    val input = new File("/home/alexandre/Projects/Test_Files/Caminhao.txt")
    val output = new File("/home/alexandre/Projects/Test_Files/Alchemy-scala_Caminhao.list")
    alchemyClient.evaluate(input, output)

    val inputEng = new File("/home/alexandre/Projects/Test_Files/Germany.txt")
    val outputEng = new File("/home/alexandre/Projects/Test_Files/Alchemy-scala_Germany.list")
    alchemyClient.evaluate(inputEng, outputEng)
  }

}