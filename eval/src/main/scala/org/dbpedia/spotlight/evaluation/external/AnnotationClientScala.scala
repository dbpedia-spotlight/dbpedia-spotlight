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

import org.apache.commons.logging.LogFactory
import org.apache.commons.httpclient.{HttpException, DefaultHttpMethodRetryHandler, HttpMethod, HttpClient}
import org.apache.commons.httpclient.params.HttpMethodParams
import org.dbpedia.spotlight.exceptions.AnnotationException
import java.io.{PrintWriter, IOException, File}
import org.apache.http.HttpStatus
import org.dbpedia.spotlight.model.{Text, DBpediaResource}
import java.text.ParseException
import scala.io.Source

;

/**
 * Abstract class for simple web service-based annotation scala clients.
 * Translate from AnnotationClient.java (the java clients were discontinued)
 *
 * Author: Pablo Mendes (original java version). Leandro Bianchini (scala translation) and Alexandre Cançado Cardoso (scala bug fixing)
 * Created: 24th/06/13
 * Last Modified: 23th/08/13
 */

abstract class AnnotationClientScala {

  val LOG = LogFactory.getLog(this.getClass)

  @throws(classOf[AnnotationException])
  def extract(text: Text): List[DBpediaResource]

  @throws(classOf[AnnotationException])
  def request(method: HttpMethod): String = {

    var response: String = null

    method.getParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false))

    try {
      val statusCode = AnnotationClientScala.client.executeMethod(method)

      if (statusCode != HttpStatus.SC_OK)
        LOG.error("Method failed: " + method.getStatusLine)

      val responseBody = method.getResponseBody

      response = new String(responseBody)
    } catch {
      case e: HttpException =>
        LOG.error("Fatal protocol violation: " + e.getMessage)
        throw new AnnotationException("Protocol error executing HTTP request.", e)
      case e: IOException =>
        LOG.error("Fatal transport error: " + e.getMessage)
        LOG.error(method.getQueryString)
        throw new AnnotationException("Transport error executing HTTP request.", e)
    }

    response
  }

  @throws(classOf[Exception])
  def saveExtractedEntitiesSet(inputFile: File, outputFile: File, parser: AnnotationClient.LineParser, restartFrom: Int) {

    val out: PrintWriter = new PrintWriter(outputFile)

    LOG.info("Opening input file " + inputFile.getAbsolutePath)

    val text: String = AnnotationClientScala.readFileAsString(inputFile)

    var i       : Int = 0
    var correct : Int = 0
    var error   : Int = 0
    var sum     : Long = 0

    for (snippet <- text.split("\n")) {
        val s: String = parser.parse(snippet)

        if (!Option(s).getOrElse("").isEmpty) {
          i += 1

          if (i >= restartFrom) {
            var entities: List[DBpediaResource] = null

            try {
              val startTime: Long = System.nanoTime()
              entities = extract(new Text(snippet.replaceAll("\\s+", " ")))
              val endTime: Long = System.nanoTime()
              sum = endTime - startTime
              LOG.info("("+i+") Extraction ran in "+sum+" ns.")
              correct += 1
            }
            catch {
              case e: AnnotationException =>
                error += 1
                LOG.error(e)
            }
            for (e <- entities) {
              out.println(e.uri)
            }
            out.println()
            out.flush()
          }
        }

    }

    out.close()
    LOG.info("Extracted entities from "+i+" text items, with "+correct+" successes and "+error+" errors.")
    LOG.info("Results saved to: "+outputFile.getAbsolutePath)
    //val avg: Double = ((sum / i) * 1000000)
    LOG.info("Average extraction time: "+((sum / i) * 1000000)+" ms.")

  }

  @throws(classOf[Exception])
  def evaluateManual(inputFile: File, outputFile: File, restartFrom: Int)  {
       saveExtractedEntitiesSet(inputFile, outputFile, new AnnotationClient.LineParser.ManualDatasetLineParser(), restartFrom)
  }

  @throws(classOf[Exception])
  def evaluate(inputFile: File, outputFile: File)  {
      evaluateManual(inputFile, outputFile, 0)
  }
}


object AnnotationClientScala {

  val client: HttpClient = new HttpClient()

  def readFileAsString(filePath: String): String = readFileAsString(new File(filePath))

  def readFileAsString(file: File): String = {

    val buffer = Source.fromFile(file.getAbsolutePath)
    buffer.mkString

  }

  abstract class LineParser {

    @throws(classOf[ParseException])
    def parse(s: String): String

    object ManualDatasetLineParser extends LineParser {

      @throws(classOf[ParseException])
      def parse(s: String): String = s.trim()

    }

    object OccTSVLineParser extends LineParser {

      @throws(classOf[ParseException])
      def parse(s: String): String = {

        var result: String = null

        try
          result = s.trim.split("/t")(3)
        catch {
          case e: ArrayIndexOutOfBoundsException => throw new ParseException(e.getMessage, 3)
        }

        result
      }

    }
  }

}
