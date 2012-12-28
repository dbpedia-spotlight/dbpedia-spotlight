package org.dbpedia.spotlight.topical.wikipedia.util

import org.dbpedia.spotlight.model.DBpediaCategory
import io.Source

/**
 * Simple loader object for topical concepts (from file) extracted by dbpedia spotlight
 */
object TopicalConceptLoader {

    def loadTopicalConcepts(path: String): Set[DBpediaCategory] = {
        var result = Set[DBpediaCategory]()

        Source.fromFile(path).getLines().foreach(line => {
            result += (new DBpediaCategory(line.substring(line.indexOf("<") + 1, line.indexOf(">")).replaceAll(".+/", "")))
        })

        result
    }

}
