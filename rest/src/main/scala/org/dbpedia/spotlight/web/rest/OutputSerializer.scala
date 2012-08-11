package org.dbpedia.spotlight.web.rest

import org.dbpedia.spotlight.model.{TopicDescription, Topic, DBpediaResource, Text}
import net.liftweb.json._
import java.io.File
import org.apache.commons.logging.LogFactory

/**
 * Object to serialize our objects and lists of objects
 *
 * @author pablomendes
 */

object OutputSerializer {

    private val LOG = LogFactory.getLog(getClass)

    def tagsAsJson(text: Text, tags: Seq[(DBpediaResource,Double)]) = {
        import net.liftweb.json._
        import net.liftweb.json.JsonDSL._
        val values = tags.map(t => (t._1.uri,t._2)) //TODO unnecessary iteration. should convert directly from DBpediaResource
        compact(render(values))
    }

    def tagsAsXml(text: Text, tags: Seq[(DBpediaResource,Double)]) = {
        <Annotation text={text.text}>
            <Resources>
               {for ((resource,score) <- tags) yield <Resource similarityScore={score.toString}>{resource.uri}</Resource>}
            </Resources>
        </Annotation>
    }

    def topicTagsAsJson(text: Text, tags: Array[(Topic,Double)]) = {
        import net.liftweb.json._
        compact(render(Xml.toJson(topicTagsAsXml(text,tags))))
    }

    def topicTagsAsXml(text: Text, tags: Array[(Topic,Double)]) = {
        val descriptions: Seq[TopicDescription] = null//Server.getConfiguration.getTopicalClassificationConfiguration.getDescription
        if (descriptions == null)
            LOG.warn("No topic descriptions were loaded, because they were not defined or defined wrong in the configuration => No iptc mediatopics found.")
        <Annotation text={text.text}>
            <Topics>
                {for ((topic,score) <- tags) yield <Topic score={score.toString} mediatopics={
                    if (descriptions!=null)
                        descriptions.find(_.topic.equals(topic)).getOrElse(new TopicDescription(null,null,Set[String](),null)).iptcTopics.reduceLeft(_ +","+_)
                    else
                        "No iptc mediatopics found"
            }>{topic.getName}</Topic> }
            </Topics>
        </Annotation>
    }
}