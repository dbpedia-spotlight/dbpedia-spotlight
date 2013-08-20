package org.dbpedia.spotlight.evaluation.external

import org.dbpedia.spotlight.exceptions.AnnotationException
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.NameValuePair
import org.dbpedia.spotlight.model.{DBpediaResource, Text}
import org.w3c.dom.{NodeList, Element, Node}
import org.dbpedia.spotlight.string.XmlParser
import java.io.{FileReader, File, IOException}
import org.xml.sax.SAXException
import javax.xml.parsers.ParserConfigurationException
import scala.collection.mutable.MutableList
import scala.collection.immutable.List
import scala.io._


/**
  * Created with IntelliJ IDEA.
  * User: Leandro Bianchini
  * Date: 24/06/13
  * Time: 22:22
  * Fixed by: Alexandre Cançado Cardoso
  * Date 19/08/13
  * To change this template use File | Settings | File Templates.
  */
class AlchemyClientScala(apikey: String) extends AnnotationClientScala {

  val url: String = "http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities"

  @throws(classOf[AnnotationException])
  def process(text: String): String = {
    val method: PostMethod = new PostMethod(url)
    method setRequestHeader("Content-type","application/x-www-form-urlencoded")

    val params: Array[NameValuePair] = Array(new NameValuePair("text", text), new NameValuePair("apikey", apikey))
    method setRequestBody(params)

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

//    for (n <- 0 to (list.getLength-1)) {
//
//      val node = list.item(n)
//
//      val attibutes: NodeList = node.getChildNodes
//
//      for (i <- 0 to (attibutes.getLength-1)) {
//
//        val att = attibutes.item(i)
//
//        val name: String = att.getNodeName
//        var value: String = ""
//
//        if (att.getNodeType != Node.TEXT_NODE) {
//          value = att.getFirstChild.getNodeValue
//          LOG.trace("Name:$name%s, Value: $value%s")
//        }
//
//        if (name.equals("text"))
//          entities :+ new DBpediaResource(value)
//      }
//    }

    var i : Int = 0
    for(i <- 0 to list.getLength-1){
      var attributes = list.item(i).getChildNodes
      //print(list.item(i).getAttributes)
      var j : Int = 0
      for(j <- 0 to attributes.getLength-1){
        val n = attributes.item(j)
        val name : String = n.getNodeName
        var value : String = ""
        if (n.getNodeType != Node.TEXT_NODE) {
          value = n.getFirstChild.getNodeValue
          LOG.trace(String.format("Name:%s, Value: %s", name, value))
        }
        if (name.equals("text")) {
          val entity = new DBpediaResource(value)
          entities = entities :+ entity
        }
      }

    }

/*Java Code
      for(int i=0; i<list.getLength(); i++) {
      NodeList attributes = list.item(i).getChildNodes();
      //System.out.println(list.item(i).getAttributes());
      for(int j=0; j<attributes.getLength(); j++) {
        Node n = attributes.item(j);
        String name = n.getNodeName();
        String value = "";
        if (n.getNodeType()!=Node.TEXT_NODE) {
          value = n.getFirstChild().getNodeValue();
          LOG.trace(String.format("Name:%s, Value: %s",name,value));
        }
        if (name.equals("text")) {
          entities.add(new DBpediaResource(value)); //TODO could have actually gotten DBpediaResourceOccurrences and set the relevance
        }
      }
    }*/
    LOG.debug("Extracted: $entities%s")
    entities

  }
}

object AlchemyClientScala {

  def main(args: Array[String]) {
    val apikey: String = "874772365fff377eb64e37c98f47392b72383d5a"  //args(0)

    val alchemyClient = new AlchemyClientScala(apikey)

//    val manualEvalInput   = new File("/Users/leandro/Documents/Projetos/dbpedia-spotlight/files/AnnotationText-Alchemy.txt.list")
//    val manualEvalOutput  = new File("/Users/leandro/Documents/Projetos/dbpeda-spotlight/files/AnnotationText.txt")
//
//    val cucerzanEvalInput  = new File("/Users/leandro/Documents/Projetos/dbpedia-spotlight/files/cucerzan.txt")
//    val cucerzanEvalOutput = new File("/Users/leandro/Documents/Projetos/dbpedia-spotlight/files/cucerzan-Alchemy2.set")
//
//    val input  = new File("/Users/leandro/Documents/Projetos/dbpedia-spotlight/files/paragraphs.txt")
//    val output = new File("/Users/leandro/Documents/Projetos/dbpedia-spotlight/files/Alchemy.list")

    val input = new File("/home/alexandre/Projects/Test_Files/Caminhao_com_ceramica_tomba_na_via_dutra.txt")
    val output = new File("/home/alexandre/Projects/Test_Files/Alchemy-scala_Caminhao_com_ceramica_tomba_na_via_dutra.list")
    alchemyClient.evaluate(input, output)
  }

}
