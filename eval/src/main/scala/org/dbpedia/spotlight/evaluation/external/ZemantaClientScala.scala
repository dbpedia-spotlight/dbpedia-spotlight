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

import scala.Predef.String
import scala.collection.immutable.List
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.exceptions.AnnotationException
import org.dbpedia.spotlight.model.DBpediaResource
import org.dbpedia.spotlight.model.Text
import org.dbpedia.spotlight.string.XmlParser
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import javax.xml.parsers.ParserConfigurationException
import java.io._
import java.util.ArrayList
import scala._

/**
 * @author pablomendes and Alexandre Cançado Cardoso (translation to scala)
 */
object ZemantaClientScala {
  def main(args: Array[String]) {

    val api_key: String = args(0)
    val c: ZemantaClient = new ZemantaClient(api_key)

//    val input: File = new File("/home/pablo/eval/grounder/gold/g1b_spotlight.txt")
//    val output: File = new File("/home/pablo/eval/grounder/systems/Zemanta.list")

    val input: File = new File("/home/alexandre/Projects/Test_Files/Caminhao_com_ceramica_tomba_na_via_dutra.txt")
    val output: File = new File("/home/alexandre/Projects/Test_Files/Zemanta-scala_Caminhao_com_ceramica_tomba_na_via_dutra.list")

    c.evaluate(input, output)
  }
}

class ZemantaClientScala(api_key: String) extends AnnotationClientScala {
  /**
   * DISCLAIMER these are not really promised by Zemanta to be DBpediaEntities. We extract them from wikipedia links.
   */
  def extract(text: Text): List[DBpediaResource] = {

                     null
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