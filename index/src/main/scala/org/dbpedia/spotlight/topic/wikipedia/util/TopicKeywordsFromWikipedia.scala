package org.dbpedia.spotlight.topic.wikipedia.util

import io._
import scala.collection.mutable._
import xml.XML
import org.dbpedia.spotlight.util.TextVectorizer
import java.io.{FileWriter, PrintWriter}
import org.apache.commons.logging.LogFactory

/**
 * This object allows you to extract keywords from the wikipedia for given topics specified in the topics file which has
 * the form: <br/>
 * topic=identifier1,identifier2...(from iptc)=keyword1,keyword2,...(some manually written keywords for these topics) <br/><br/>
 * It extracts category members, lists of the topic, outlines of the topic, indexes of the topic and glossaries of the topic for
 * each specified keyword, and merges everything together to find the best keywords for one topic
 */
object TopicKeywordsFromWikipedia {

  private val LOG = LogFactory.getLog(getClass)

  /**
   *
   * @param args input, output
   */
  def main(args: Array[String]) {
    writeVectors("/home/dirk/workspace/dbpedia-spotlight/index/src/main/resources/topics.list", "/media/Data/Wikipedia/topics.vectors")
  }

  /**
   *
   * @param inputFile the input topic file
   * @param outputFile
   */
  def writeVectors(inputFile: String, outputFile: String) {
    val writer = new PrintWriter(new FileWriter(outputFile))
    val vectorizer = new TextVectorizer()
    val topicVectors = Map[String, List[(String, Double)]]()
    val idf = Map[String, Double]()

    var counter = 0
    Source.fromFile(inputFile).getLines().foreach(topicDescription => {
      val split = topicDescription.split("=")
      var vector: List[(String, Double)] = null
      var text = ""

      split(2).split(",").foreach(topicName => {
        var response = XML.loadString(WikipediaApiRequest.request("list=categorymembers&cmtitle=Category:" + topicName.trim + "&cmlimit=5000")) \\ "cm"

        text += response.foldLeft("")((acc, node) => acc + " " + (node \\ "@title").text)

        response = XML.loadString(WikipediaApiRequest.request("prop=extracts&titles=Lists_of_" + topicName.trim)) \\ "extract"
        text += response.text

        response = XML.loadString(WikipediaApiRequest.request("prop=extracts&titles=Outline_of_" + topicName.trim)) \\ "extract"
        text += response.text

        response = XML.loadString(WikipediaApiRequest.request("prop=extracts&titles=Index_of_" + topicName.trim + "_articles")) \\ "extract"
        text += response.text

        response = XML.loadString(WikipediaApiRequest.request("prop=extracts&titles=Glossary_of_" + topicName.trim)) \\ "extract"
        text += response.text

        response = XML.loadString(WikipediaApiRequest.request("prop=extracts&titles=Glossary_of_" + topicName.trim + "_terms")) \\ "extract"

        text += response.text + " "
      })

      if (!text.matches("[ ]+")) {
        vector = vectorizer.getWordCountVector(text.replaceAll("<[^>]*>", " "))
          .filter(entry => entry._2 > 1 && !entry._1.equals("list") &&
          !entry._1.equals("unit") &&
          !entry._1.contains("index") &&
          !entry._1.contains("outlin") &&
          !entry._1.equals("branch") &&
          !entry._1.equals("follow") &&
          !entry._1.equals("topic") &&
          !entry._1.equals("see") &&
          !entry._1.contains("articl") &&
          !entry._1.equals("main") &&
          !entry._1.equals("peopl") &&
          !entry._1.contains("perso") &&
          !entry._1.equals("link") &&
          !entry._1.equals("men") &&
          !entry._1.equals("man") &&
          !entry._1.contains("woman") &&
          !entry._1.contains("child") &&
          !entry._1.contains("women") &&
          !entry._1.contains("associ") &&
          !entry._1.contains("organ") &&
          !entry._1.contains("websit") &&
          !entry._1.contains("page") &&
          !entry._1.contains("famil") &&
          !entry._1.equals("new") &&
          !entry._1.equals("summer") &&
          !entry._1.equals("winter") &&
          !entry._1.equals("data") &&
          !entry._1.equals("ident") && !entry._1.equals("share") && //confuses society
          !entry._1.equals("school") && //confuses medicine
          (split(0).contains("history") ||
            !entry._1.contains("histori")) &&
          (!split(0).contains("video_game") ||
            !entry._1.contains("chronolog"))).toList.sortBy(-_._2).take(100)


        //topic words are usually too strong and do not give other words much value
        val topicWords = vectorizer.getWordCountVector(split(0).toLowerCase.replaceAll("[^a-z]", " "))

        vector = vector.filter(entry => !(topicWords.contains(entry._1)))

        topicVectors += (split(0).trim + " " + split(1).trim -> vector)

        vector.foreach(word => {
          if (!idf.contains(word._1))
            idf += (word._1 -> 1)
          else
            idf(word._1) += 1
        })
      }

      counter += 1
      if (counter % 10 == 0)
        LOG.info(counter + " outlines/indexes processed")
    })
    topicVectors.foreach {
      case (topic, vector) => {
        var vector2 = vector.map[(String, Double), List[(String, Double)]] {
          case (word, value) =>
            (word, math.sqrt(value) * math.log(topicVectors.size / idf.getOrElse(word, topicVectors.size.toDouble)))
        }
        val sum = math.sqrt(vector2.foldLeft(0.0)((acc, value) => acc + value._2 * value._2))

        vector2 = vector2.map[(String, Double), List[(String, Double)]] {
          case (word, value) => (word, value / sum)
        }

        writer.print(topic)
        vector2.sortBy(-_._2).foreach {
          case (word, value) => writer.print(" " + word + ":" + value)
        }
        writer.println()
      }
    }
    writer.close()
  }
}
