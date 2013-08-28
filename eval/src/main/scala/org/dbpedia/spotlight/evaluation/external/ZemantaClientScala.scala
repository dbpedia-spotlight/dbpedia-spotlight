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

import scala.collection.immutable.List
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{DBpediaResource, Text}
import org.dbpedia.spotlight.string.XmlParser
import org.w3c.dom.{NodeList, Element, Node}
import org.xml.sax.SAXException
import javax.xml.parsers.ParserConfigurationException
import java.io.{File, IOException}

/**
 * Simple web service-based annotation scala client for Zemanta.
 * Translate from DBpediaSpotlightClient.java (the java clients were discontinued)
 *
 * Author: Pablo Mendes (original java version) and Alexandre Cançado Cardoso (scala translation)
 * Created: 08/13
 * Last Modified: 27th/08/13
 *
 * Tested for English and Portuguese, ok for English only.
 * (Zemanta service support only English language.)
 */

class ZemantaClientScala(api_key: String) extends AnnotationClientScala {

  override val LOG: Log = LogFactory.getLog(classOf[ZemantaClientScala])

  /**
   * DISCLAIMER these are not really promised by Zemanta to be DBpediaEntities. We extract them from wikipedia links.
   */
  def extract(text: Text): List[DBpediaResource] = {
    val response: String = process(text.text)
    var entities: List[DBpediaResource] = List[DBpediaResource]()

    try {
      val root: Element = XmlParser.parse(response)
      val xpath: String = "//markup/links/link/target[type='wikipedia']/url"
      val list: NodeList = XmlParser.getNodes(xpath, root)
      val listLength : Int = list.getLength
      LOG.info("Entities returned: "+list.getLength)

      for(i <- 0 to list.getLength-1){
        val n: Node = list.item(i)
        val name: String = n.getNodeName
        val value: String = n.getFirstChild.getNodeValue.replaceAll("http://en.wikipedia.org/wiki/", "")

        entities = entities :+ new DBpediaResource(value)
      }

      print("\n")
    }catch {
      case e: IOException => e.printStackTrace
      case e: SAXException => e.printStackTrace
      case e: ParserConfigurationException => e.printStackTrace
    }

    entities
  }

  protected def process(text: String): String = {
    val url: String = "http://api.zemanta.com/services/rest/0.0/"
    val method: PostMethod = new PostMethod(url)
    method.setRequestHeader("Content-type", "application/x-www-form-urlencoded")

    val params: Array[NameValuePair] = Array(new NameValuePair("method", "zemanta.suggest"), new NameValuePair("api_key", api_key), new NameValuePair("text", text), new NameValuePair("format", "xml"))

    method.setRequestBody(params)
    LOG.debug("Sending request to Zemanta: " + params)

    val response: String = request(method)

    response
  }

}


object ZemantaClientScala {

  def main(args: Array[String]) {

    val api_key: String = args(0)
    val c = new ZemantaClientScala(api_key)

    val input: File = new File("/home/alexandre/Projects/Test_Files/Caminhao.txt")
    val output: File = new File("/home/alexandre/Projects/Test_Files/Zemanta-scala_Caminhao.list")
    c.evaluate(input, output)

    val inputEng: File = new File("/home/alexandre/Projects/Test_Files/Germany.txt")
    val outputEng: File = new File("/home/alexandre/Projects/Test_Files/Zemanta-scala_Germany.list")
    c.evaluate(inputEng, outputEng)

  }

}