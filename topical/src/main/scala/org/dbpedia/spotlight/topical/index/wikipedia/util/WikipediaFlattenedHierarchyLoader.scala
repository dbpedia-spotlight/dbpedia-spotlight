package org.dbpedia.spotlight.feed.wikipedia.util

import org.apache.commons.logging.LogFactory
import java.io.File
import io.Source
import org.dbpedia.spotlight.model.{DBpediaCategory}
import scala.collection.mutable._
import org.dbpedia.spotlight.feed.Topic

/**
 * Utility object which loads the flattened hierarchy into memory.
 */
object WikipediaFlattenedHierarchyLoader {

    private val LOG = LogFactory.getLog(getClass)

    def loadFlattenedHierarchy(flattenedHierarchyDir: File): Map[Topic, Map[DBpediaCategory, Double]] = {
        var flattenedHierarchy = Map[Topic, Map[DBpediaCategory, Double]]()

        if (flattenedHierarchyDir.exists()) {
            flattenedHierarchyDir.listFiles().foreach(topicFile => {
                LOG.info("Loading " + topicFile.getAbsolutePath)
                var set = Map[DBpediaCategory, Double]()
                Source.fromFile(topicFile).getLines().foreach(line => {
                    set += (new DBpediaCategory(line.split("\t")(0)) -> line.split("\t")(1).toDouble)
                })

                val name = topicFile.getName.substring(0, topicFile.getName.length - 4)
                flattenedHierarchy += (new Topic(name) -> set)
            })
            LOG.info("Flattened hierarchy was loaded!")
        }
        else
            LOG.warn("Flattened hierarchy was not found, loaded empty flattened hierarchy!")

        flattenedHierarchy
    }

}
