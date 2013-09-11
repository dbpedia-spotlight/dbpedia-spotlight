package org.dbpedia.spotlight.web.rest

import org.dbpedia.spotlight.model._
import net.liftweb.json._
import java.io.File

/**
 * Object to serialize our objects and lists of objects
 *
 * @author pablomendes
 */

object OutputSerializer {

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
}