package org.dbpedia.spotlight.web.rest

import org.dbpedia.spotlight.lucene.LuceneManager
import java.io.File
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache}
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.extract.LuceneTagExtractor
import sjson.json._
import DefaultProtocol._
import JsonSerialization._
import org.dbpedia.spotlight.model.{SurfaceFormOccurrence, DBpediaResource, Text, SpotlightConfiguration}

/**
 * Object to serialize our objects and lists of objects
 *
 * @author pablomendes
 */

object OutputSerializer {

    def tagsAsJson(text: Text, tags: Seq[(DBpediaResource,Double)]) = {
        val values = tags.map(t => (t._1.uri,t._2)) //TODO unnecessary iteration. should convert directly from DBpediaResource
        tojson(values).toString()
    }

    def tagsAsXml(text: Text, tags: Seq[(DBpediaResource,Double)]) = {
        <Annotation text={text.text}>
            <Resources>
               {for ((resource,score) <- tags) yield <Resource similarityScore={score.toString}>{resource.uri}</Resource>}
            </Resources>
        </Annotation>
    }
}