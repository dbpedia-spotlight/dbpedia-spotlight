package org.dbpedia.spotlight.model

import java.net.URL
import xml.XML

/**
 * Loads a topic description xml file.<br>
 * Example entry in such a file:
 * <br>
 * &lt;topic name="biology"&gt;          <br>
 *   &lt;iptc mediatopic="20000719"/&gt;       <br>
 *   &lt;keywords&gt;                            <br>
 *     biomechanics,ecology,ecologist,gene,cell,cellular,biologist,bacteria,evolution,dna,rna,protein,chromosome,genetic,biological,bionics,plant,botany,flora,botanist  <br>
 *   &lt;/keywords&gt;       <br>
 *   &lt;feed url="http://feeds.biologynews.net/biologynews/headlines?format=xml"/--&gt;      <br>
 * &lt;/topic&gt;     <br>
 */
object TopicDescription {

  def fromDescriptionFile(file: String): Seq[TopicDescription] = {
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

      new TopicDescription(topic, keywords, iptcTopics, feeds)
    }
  }
}

class TopicDescription(val topic: Topic, val keywords: Seq[String], val iptcTopics: Set[String], val rssFeeds: Set[URL])