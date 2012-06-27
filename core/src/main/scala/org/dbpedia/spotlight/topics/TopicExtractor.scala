/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dbpedia.spotlight.topics

import org.apache.commons.httpclient.{HttpStatus, DefaultHttpMethodRetryHandler, HttpClient}
import org.apache.commons.httpclient.params.HttpMethodParams
import org.apache.http.HttpException
import java.io.IOException
import org.apache.commons.httpclient.methods.GetMethod
import java.net.URLEncoder
import net.liftweb.json._


/**
 *
 * @author pablomendes
 */

object TopicExtractor {

    val client = new HttpClient
    val url_pattern = "http://160.45.137.73:2222/rest/topic?text=%s"

    def getTopics(text: String) : Map[String,Double] = {

        val url = String.format(url_pattern, URLEncoder.encode(text, "UTF8"))
        val method = new GetMethod(url)
        method.getParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false))

        var response = ""
        try {
            val statusCode: Int = client.executeMethod(method)
            if (statusCode != HttpStatus.SC_OK) {
                println("Method failed: " + method.getStatusLine)
            }
            val responseBody: Array[Byte] = method.getResponseBody
            response = new String(responseBody)
        }
        catch {
            case e: HttpException => {
                println("Fatal protocol violation: " + e.getMessage)
            }
            case e: IOException => {
                println("Fatal transport error: " + e.getMessage)
                println(method.getQueryString)
            }
        }
        finally {
            method.releaseConnection
        }

        val parsed = parse(response)
        val pairs = (parsed \\ "topic" \\ classOf[JField])
        val topics = pairs.filter(_._1.equals("@topic")).map(p => p._2.toString)
        val scores = pairs.filter(_._1.equals("@score")).map(p => p._2.toString.toDouble)
        val map = topics.zip(scores).toMap[String,Double]
        map
    }

    def main(args: Array[String]) {

        val text = "basketball michael jordan"

        val response = getTopics(text)
        println("Response: "+response)



    }

}
