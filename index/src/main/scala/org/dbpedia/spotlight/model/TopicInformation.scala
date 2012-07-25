package org.dbpedia.spotlight.model

import java.net.URL
import xml.XML
import org.dbpedia.spotlight.topic.utility

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 7/5/12
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
object TopicInformation {

  def fromDescriptionFile(file:String):Seq[TopicInformation] = {
    val xml = XML.loadFile(file)

    for (topicItem <- xml \\ "topic") yield {
      val topic = new Topic((topicItem \\ "@name").head.text)
      val keywords = (topicItem \\ "keywords").head.text.split(",").map(_.trim)
      var iptcTopics = Set[String]()
      for (iptcItem <- topicItem \\ "iptc")
        iptcTopics += (iptcItem \\ "@mediatopic").head.text

      var feeds = Set[URL]()
      for (feedItem <- topicItem \\ "feed")
        feeds += new URL((feedItem \\ "@url").head.text)

      new TopicInformation(topic, keywords, iptcTopics, feeds)
    }
  }
}

class TopicInformation (val topic:Topic, val keywords: Seq[String], val iptcTopics:Set[String], val rssFeeds:Set[URL])