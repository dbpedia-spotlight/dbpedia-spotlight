package org.dbpedia.spotlight.topic.wikipedia.util

import org.dbpedia.spotlight.model.DBpediaCategory
import io.Source

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 7/3/12
 * Time: 4:23 PM
 * To change this template use File | Settings | File Templates.
 */

object TopicalConceptLoader {

  def loadTopicalConcepts(path:String) : Set[DBpediaCategory] = {
    var result = Set[DBpediaCategory]()

    Source.fromFile(path).getLines().foreach(line => {
      result += (new DBpediaCategory(line.substring(line.indexOf("<")+1,line.indexOf(">")).replaceAll(".+/","")))
    })

    result
  }

}
