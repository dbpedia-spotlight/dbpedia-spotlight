package org.dbpedia.spotlight.web.rest

import java.net.URLEncoder
import sys.process._
import java.io._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.dbpedia.spotlight.log.SpotlightLog
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.IllegalArgumentException

/**
 * Test if the current Spotlight is handling at least the last version entry (text or text inside a html page) size limit.
 *
 * @author Alexandre Cançado Cardoso - accardoso
 */
//TODO remove Unix cURL dependency. It was almost DONE at: https://github.com/accardoso/dbpedia-spotlight/blob/dev/test-issue72/rest/src/test/scala/org/dbpedia/spotlight/web/rest/ServerTextSizeLimitTest.scala

@RunWith(classOf[JUnitRunner])
class ServerTextSizeLimitTest extends FlatSpec with ShouldMatchers {

  // Url of the HTML file (.html) that contains the text to the test with &url
  private val url:String = "https://raw.github.com/accardoso/test-files-spotlight/master/kjb/gospelAndPartialCorIIKingJamesBible_v3.html" // 489.1 kB

  // Url with the PLAIN TEXT file (.txt) that contains the text to the test with &text
  private val textURL: String = "https://raw.github.com/accardoso/test-files-spotlight/master/kjb/pain%20text/gospelKingJamesBible.txt" // 459.5 kB

  //Test Files Repository: https://github.com/accardoso/test-files-spotlight/tree/master/kjb/ (With more test files)


  /* Tests to &url */
  private var urlResponse:ServerTextSizeLimitTestResult = null

  "A &url request" should "answer" in {
    if(urlResponse == null) urlResponse = ServerTextSizeLimitTest.testByUrl(url)

    urlResponse.getAnswered should be === true
  }

  it should "answer a valid response" in {
    if(urlResponse == null) urlResponse = ServerTextSizeLimitTest.testByUrl(url)

    SpotlightLog.info(this.getClass, "******\"?URL=\" test******\n%s\n", urlResponse.toString)
    urlResponse.getAnswered should be === true
    urlResponse.isResponseValid should be === true
  }

  it should "answer in 6 minutes" in {
    if(urlResponse == null) urlResponse = ServerTextSizeLimitTest.testByUrl(url)

    ServerTextSizeLimitTest.removeTestFilesAndFolder() //To free the disk space when auto-running the tests

    urlResponse.getAnswered should be === true
    urlResponse.getDuration should (be >= 0 and be <= 360) //360s=6min
    SpotlightLog.info(this.getClass, "\"?URL=\" test: time to answer = %ds\n", urlResponse.getDuration)
  }

  /* Tests to &text */
  private var text: String = ""
  private var textResponse:ServerTextSizeLimitTestResult = null

  "A &text request" should "answer" in {
    if(textResponse == null){
      if (text == "")
        if (textURL == "") throw new IllegalArgumentException("Value textURL can not be empty")
      text = scala.io.Source.fromURL(textURL).mkString
      textResponse = ServerTextSizeLimitTest.testByText(text)
    }

    textResponse.getAnswered should be === true
  }

  it should "answer a valid response" in {
    if(textResponse == null){
      if (text == "")
        if (textURL == "") throw new IllegalArgumentException("Value textURL can not be empty")
      text = scala.io.Source.fromURL(textURL).mkString
      textResponse = ServerTextSizeLimitTest.testByText(text)
    }

    SpotlightLog.info(this.getClass, "******\"?TEXT=\" test******\n%s\n", textResponse.toString)
    textResponse.getAnswered should be === true
    textResponse.isResponseValid should be === true
  }

  it should "answer in 6 minutes" in {
    if(textResponse == null){
      if (text == "")
        if (textURL == "") throw new IllegalArgumentException("Value textURL can not be empty")
      text = scala.io.Source.fromURL(textURL).mkString
      textResponse = ServerTextSizeLimitTest.testByText(text)
    }

    ServerTextSizeLimitTest.removeTestFilesAndFolder() //To do free the disk space when auto-running the tests

    textResponse.getAnswered should be === true
    textResponse.getDuration should (be >= 0 and be <= 360) //360s=6min
    SpotlightLog.info(this.getClass, "\"?TEXT=\" test: time to answer = %ds\n", textResponse.getDuration)
  }

}

object ServerTextSizeLimitTest extends Exception {

  // Spotlight server
  private val server = "http://localhost:2222/rest/"

  // Spotlight interface
  private val interface = "annotate"

  // Output Folder
  private val outputFolder:String = "../TextSizeTest_Output"

  /******************************* TextSizeTest Code *******************************/
  private def testCore(textSrc: String): ServerTextSizeLimitTestResult = {
    //Create the Folder and Files used in the test
    ("mkdir "+outputFolder).!
    val requestFile: File = new File(outputFolder + File.separator + "TextSizeTest"+(System.nanoTime()/1000000000)+".sh") //a bash with the http request
    val responseFile: File = new File(outputFolder + File.separator + "TextSizeTest_output_"+(System.nanoTime()/1000000000)+".xml") //with stout of the http request
    val curlLogFile: File = new File(outputFolder + File.separator + "TextSizeTest"+(System.nanoTime()/1000000000)+".err") //with stderr of the http request
    val httpParamsFile = new File(outputFolder + File.separator + "TextSizeTest_httpParams"+(System.nanoTime()/1000000000)) //with the http params if httpParamsInFile==true, else empty

    //Create a file with the request params including the text/url
    val w = new PrintWriter(httpParamsFile)
    w.write("?"+textSrc)
    w.close()

    //Write the http request command in the bash (.sh). Use cURL because has lass url length limitations than any other libraries and browsers
    val writer = new PrintWriter(requestFile)
    writer.write("curl " + " -d @"+ httpParamsFile.getCanonicalPath + " " +server + interface +"/"
      + " > " + responseFile.getCanonicalPath + " 2> " + curlLogFile.getCanonicalPath) //curl -X POST -d @filename http://hostname/resource
    writer.close()

    //Cal the bash (.sh) to run the http request to Spotlight and count the time the answer
    SpotlightLog.info(this.getClass, "Request send to Spotlight at time: %d", System.nanoTime()/1000000000)
    val beginning : Double = System.nanoTime
    ("sh " + requestFile.getCanonicalPath).!
    val duration:Int = ((System.nanoTime - beginning)/1000000000).toInt  //1s=10^(⁻9)ns
    SpotlightLog.info(this.getClass, "Response received from Spotlight at time: %d", System.nanoTime/1000000000)

    //Verify if a local command limitation produce the error.
    val curlLog:String = scala.io.Source.fromFile(curlLogFile.getCanonicalFile).mkString
    if(curlLog.contains("""curl: Argument list too long""")){
      SpotlightLog.info(this.getClass, "Although the HTTP protocol do not specify any url maximum length all transfer " +
        "libraries (as cURL) and browsers have practical limits.\n" +
        "So its not a Spotlight limitation but the text to be annotated is too long to be requested as a http url, " +
        "try to transform it in a html file and use the ?url= parameter.\n" +
        "Note: The cURL library was use because it accepted the longer url than the other browsers and libraries in our tests." +
        "More about at: http://www.boutell.com/newfaq/misc/urllength.html and http://stackoverflow.com/questions/417142/what-is-the-maximum-length-of-a-url-in-different-browsers")

      //remove the temporary files
      ("rm " +requestFile.getCanonicalPath).!
      ("rm " +curlLogFile.getCanonicalPath).!
      ("rm " +httpParamsFile.getCanonicalPath).!
      SpotlightLog.info(this.getClass, "The Spotlight response are in the output file: %s", responseFile.getCanonicalPath)

      throw new IllegalArgumentException("Client-side URL transfer library limitation: request is too long. (\"curl: Argument list too long\")")
    } else{
      //remove the temporary files
      ("rm " +requestFile.getCanonicalPath).!
      ("rm " +curlLogFile.getCanonicalPath).!
      ("rm " +httpParamsFile.getCanonicalPath).!
      SpotlightLog.info(this.getClass, "The Spotlight response are in the output file: %s", responseFile.getCanonicalPath)
    }

    //Return the text result
    if (responseFile.getUsableSpace == 0) new ServerTextSizeLimitTestResult(false, duration, responseFile.getCanonicalFile)
    else new ServerTextSizeLimitTestResult(true, duration, responseFile.getCanonicalFile)
  }

  // Send the request with &url parameter
  private def testByUrl(url: String):ServerTextSizeLimitTestResult = {
    if(url == "") throw new IllegalArgumentException("URL must not be empty")
    testCore("&url=" + url)
  }

  // Send the request with &text parameter
  private def testByText(text: String):ServerTextSizeLimitTestResult = {
    if(text == "") throw new IllegalArgumentException("Text must not be empty")
    testCore("&text=" + URLEncoder.encode(text, "utf-8"))
  }

  // Remove all files and folder created by the test
  def removeTestFilesAndFolder() = {
    ("rm -r " +outputFolder).!
  }
}

/* Abstract Data Type to this test results representation */
class ServerTextSizeLimitTestResult(answered: Boolean, duration: Int, spotlightResponse: File){
  def this() = this(false, -1, null)

  def isResponseValid:Boolean = {
    var attributeText: String = ""
    try{
      val responseXml = xml.XML.loadFile(spotlightResponse)
      val annotationText = responseXml \\ "Annotation"
      attributeText = (annotationText \ "@text").toString()
    } catch {
      case e: Exception => attributeText == ""
    }
    if(attributeText == "") false
    else true
  }

  def getAnswered:Boolean = answered
  def getDuration:Int = duration
  def getSpotlightResponse:File = spotlightResponse

  override def toString:String = {
    var str = "Spotlihgt has answered tested request: " + answered
    if(duration>=0) str += "\nTime to answer: " + duration + "s"
    if(answered) str += "\nSpotlight response at output file: " + spotlightResponse.getCanonicalPath
    str
  }
}