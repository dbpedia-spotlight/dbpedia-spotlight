package org.dbpedia.spotlight.model

import scala.collection.JavaConversions._
import org.dbpedia.spotlight.string.ModifiedWikiUtil


class DBpediaResource(var uri : String, var support : Int = 0, var types : List[DBpediaType] = List[DBpediaType]())
{
    require(uri != null)

    uri = uri.replace("http://dbpedia.org/resource/", "")

    uri = if (ModifiedWikiUtil.isEncoded(uri)) {
              ModifiedWikiUtil.spaceToUnderscore(uri)
          }
          else {
              ModifiedWikiUtil.wikiEncode(uri)
          }


    def this(uri : String) = {
        this(uri, 0, List[DBpediaType]())
    }

    def equals(that : DBpediaResource) : Boolean = {
        this.uri.equals(that.uri)
    }

    def setSupport(s : Int) {
        support = s
    }

    def setTypes(typesList : java.util.List[DBpediaType]) {
        types = typesList.toList
    }
    
    override def toString = {
        val typesString = if (types.nonEmpty) types.map(_.name).mkString("(", ",", ")") else ""
        "DBpediaResource["+uri+typesString+"]"
    }
}

