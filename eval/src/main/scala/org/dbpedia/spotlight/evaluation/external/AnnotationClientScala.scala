package org.dbpedia.spotlight.evaluation.external

import org.apache.commons.logging.LogFactory
import org.apache.commons.httpclient.{HttpException, DefaultHttpMethodRetryHandler, HttpMethod, HttpClient}
import org.apache.commons.httpclient.params.HttpMethodParams
import org.dbpedia.spotlight.exceptions.AnnotationException
import java.io.{PrintWriter, FileInputStream, IOException, File}
import org.apache.http.HttpStatus
import org.dbpedia.spotlight.model.{Text, DBpediaResource}
import java.text.ParseException

;

/**
 * Created with IntelliJ IDEA.
 * User: Leandro Bianchini
 * Date: 24/06/13
 * Time: 22:23
 * To change this template use File | Settings | File Templates.
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
    }
   catch {

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

    text.split("\n") foreach {
      snippet =>
        val s: String = parser.parse(snippet)

        if (!Option(s).getOrElse("").isEmpty) {
          i += 1

          if (i >= restartFrom) {
            var entities: List[DBpediaResource] = Nil

            try {
              val startTime: Long = System.nanoTime()
              entities = extract(new Text(snippet.replaceAll("\\s+", " ")))
              val endTime: Long = System.nanoTime()
              sum = endTime - startTime
              LOG.info("($i%f) Extraction ran in $sum%s ns.")
              correct += 1
            }
            catch {
              case e: AnnotationException =>
                error += 1
                LOG.error(e)
            }

            entities foreach {e => out.println(e.uri)}
            out.println()
            out.flush()
          }
        }

    }

    out.close()
    LOG.info("Extracted entities from $i%f text items, with $correct%s successes and $error%s errors.")
    LOG.info("Results saved to: "+outputFile.getAbsolutePath)
    val avg: Double = ((sum / i) * 1000000)
    LOG.info("Average extraction time: $avg%s ms")

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

    val buffer = Stream.continually(new FileInputStream(file).read).takeWhile(-1 !=).map(_.toByte).toArray
    buffer.toString

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
