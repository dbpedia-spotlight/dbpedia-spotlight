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

import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.methods.GetMethod
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import scala.Predef._
import org.dbpedia.spotlight.exceptions.AnnotationException
import org.dbpedia.spotlight.model.{DBpediaResource, Text}
import java.io.File
import scala.collection.immutable.List

/**
 * Simple web service-based annotation scala client for DBpedia Spotlight.
 * Translate from DBpediaSpotlightClient.java (the java clients were discontinued)
 *
 * Author: Pablo Mendes and Joachim Daiber (original java version) and Alexandre Cançado Cardoso (scala translation)
 * Created: 08/13
 * Last Modified: 23th/08/13
 *
 * Tested for English and Portuguese, ok for English only. To use for any other language, changes in url and
 * modifications at services output manipulations is needed, for correct outputs.
 */

class DBpediaSpotlightClientScala extends AnnotationClientScala {

  private val API_URL: String = "http://spotlight.dbpedia.org/"
  //private val API_URL: String = "http://107.20.250.248:2222/" //for portuguese. Need to modify output format to exclude the dbpedia link pre-appended
  private val CONFIDENCE: Double = 0.0
  private val SUPPORT: Int = 0

  def extract(text: Text): List[DBpediaResource] = {
    LOG.info("Querying API.")

    var spotlightResponse: String = null

    try {
      val getMethod: GetMethod = new GetMethod(API_URL + "rest/annotate/?" + "confidence=" + CONFIDENCE + "&support=" + SUPPORT + "&text=" + URLEncoder.encode(text.text, "utf-8"))
      getMethod.addRequestHeader(new Header("Accept", "application/json"))

      spotlightResponse = request(getMethod)
    } catch {
      case e: UnsupportedEncodingException => throw new AnnotationException("Could not encode text.", e)
    }

    assert(spotlightResponse != null)

    var resultJSON: JSONObject = null
    var entities: JSONArray = null

    try {
      resultJSON = new JSONObject(spotlightResponse)
      entities = resultJSON.getJSONArray("Resources")
    } catch {
      case e: JSONException => throw new AnnotationException("Received invalid response from DBpedia Spotlight API.")
    }

    var resources: List[DBpediaResource] = List[DBpediaResource]()

    for(i <- Range.inclusive(0, entities.length()-1)) {
      try {
        val entity: JSONObject = entities.getJSONObject(i)
        resources = resources :+ new DBpediaResource(entity.getString("@URI"), Integer.parseInt(entity.getString("@support")))
      } catch {
        case e: JSONException => LOG.error("JSON exception " + e)
      }
    }

    resources
  }

}


object DBpediaSpotlightClientScala {

  def main(args: Array[String]) {

    val c: DBpediaSpotlightClientScala = new DBpediaSpotlightClientScala

    val input: File = new File("/home/alexandre/Projects/Test_Files/Caminhao.txt")
    val output: File = new File("/home/alexandre/Projects/Test_Files/Spotlight-scala_Caminhao.list")
    c.evaluate(input, output)

    val inputEng: File = new File("/home/alexandre/Projects/Test_Files/Germany.txt")
    val outputEng: File = new File("/home/alexandre/Projects/Test_Files/Spotlight-scala_Germany.list")
    c.evaluate(inputEng, outputEng)

  }

}