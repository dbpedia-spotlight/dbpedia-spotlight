package org.dbpedia.spotlight.model

import java.net.URL
import java.io.File
import xml.XML

/**
 * Loads a topic description xml file.<br>
 * Example entry in such a file:
 * <br>
 * &lt;topic name="biology"&gt;          <br>
 * &lt;iptc mediatopic="20000719"/&gt;       <br>
 * &lt;keywords&gt;                            <br>
 * biomechanics,ecology,ecologist,gene,cell,cellular,biologist,bacteria,evolution,dna,rna,protein,chromosome,genetic,biological,bionics,plant,botany,flora,botanist  <br>
 * &lt;/keywords&gt;       <br>
 * &lt;feed url="http://feeds.biologynews.net/biologynews/headlines?format=xml"/--&gt;      <br>
 * &lt;/topic&gt;     <br>
 */
object TopicDescription {

    def fromDescriptionFile(file: File): Seq[TopicDescription] = {
        val xml = XML.loadFile(file)

        for (topicItem <- xml \\ "topic") yield {
            val topic = new Topic((topicItem \\ "@name").head.text) // HACK: bug fix    Computer_science got read with more than 16 characters
            val categories = (topicItem \\ "categories").head.text.split(",").map(category => category.toCharArray.subSequence(0, category.length).toString.trim)
            //val keywords = (topicItem \\ "keywords").head.text.split(",").map(category => category.toCharArray.subSequence(0, category.length).toString.trim)

            var iptcTopics = Set[String]()
            for (iptcItem <- topicItem \\ "iptc")
                iptcTopics += (iptcItem \\ "@mediatopic").head.text

            var feeds = Set[URL]()
            for (feedItem <- topicItem \\ "feed")
                feeds += new URL((feedItem \\ "@url").head.text)

            TopicDescription(topic, categories, iptcTopics, feeds)
        }
    }
}

case class TopicDescription(topic: Topic, categories: Seq[String],iptcTopics: Set[String], rssFeeds: Set[URL])