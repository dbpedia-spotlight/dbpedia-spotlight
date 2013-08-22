
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
 * Simple web service-based annotation client for DBpedia Spotlight.
 *
 * @author pablomendes, Joachim Daiber, Alexandre Cançado Cardoso (translation to scala)
 *
 */

object DBpediaSpotlightClientScala {
  def main(args: Array[String]) {
    val c: DBpediaSpotlightClientScala = new DBpediaSpotlightClientScala

//    val input: File = new File("/home/pablo/eval/csaw/gold/paragraphs.txt")
//    val output: File = new File("/home/pablo/eval/csaw/systems/Spotlight.list")

    val input: File = new File("/home/alexandre/Projects/Test_Files/Caminhao_com_ceramica_tomba_na_via_dutra.txt")
    val output: File = new File("/home/alexandre/Projects/Test_Files/Spotlight-scala_Caminhao_com_ceramica_tomba_na_via_dutra.list")

//    val input: File = new File("/home/alexandre/Projects/Test_Files/annotation1.txt")
//    val output: File = new File("/home/alexandre/Projects/Test_Files/Spotlight-scala_annotation1.list")

    c.evaluate(input, output)
  }
}

class DBpediaSpotlightClientScala extends AnnotationClientScala {
  //private val API_URL: String = "http://spotlight.dbpedia.org/"
  private val API_URL: String = "http://107.20.250.248:2222/"     //todo: modify output format to exclude the dbpedia link
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

    //for (i <- 0 to entities.length-1) {
    //var i:Int = 0
    //for (aux <- entities) {
    for(i <- Range.inclusive(0, entities.length()-1)) {
      try {
        val entity: JSONObject = entities.getJSONObject(i)
        resources = resources :+ new DBpediaResource(entity.getString("@URI"), Integer.parseInt(entity.getString("@support")))
      } catch {
        case e: JSONException => LOG.error("JSON exception " + e)
      }
      //i +=1
    }

    resources
  }
}

