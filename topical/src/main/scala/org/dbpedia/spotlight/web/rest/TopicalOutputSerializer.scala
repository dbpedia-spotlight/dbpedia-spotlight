package org.dbpedia.spotlight.web.rest

import net.liftweb.json._
import org.dbpedia.spotlight.model.{TopicDescription, Topic, Text}
import org.dbpedia.spotlight.log.SpotlightLog

/**
 * @author dirk
 */
object TopicalOutputSerializer {
    def topicTagsAsJson(text: Text, tags: Array[(Topic,Double)]) = {
        compact(render(Xml.toJson(topicTagsAsXml(text,tags))))
    }

    def topicTagsAsXml(text: Text, tags: Array[(Topic,Double)]) = {
        val descriptions: Seq[TopicDescription] = null//Server.getConfiguration.getTopicalClassificationConfiguration.getDescription
        if (descriptions == null)
            SpotlightLog.warn(this.getClass, "No topic descriptions were loaded, because they were not defined or defined wrong in the configuration => No iptc mediatopics found.")
        <Annotation text={text.text}>
            <Topics>
                {for ((topic,score) <- tags) yield <Topic score={score.toString} mediatopics={
            if (descriptions!=null)
                descriptions.find(_.topic.equals(topic)).getOrElse(TopicDescription(null,null,Set[String](),null)).iptcTopics.reduceLeft(_ +","+_)
            else
                "No iptc mediatopics found"
            }>{topic.getName}</Topic> }
            </Topics>
        </Annotation>
    }
}
