package org.dbpedia.spotlight.util

import java.io.File
import io.Source
import scala.collection.JavaConversions._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{DBpediaResource, DBpediaType}
import java.util.{LinkedHashSet, LinkedList}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 30.08.2010
 * Time: 13:08:23
 * To change this template use File | Settings | File Templates.
 */

object TypesLoader
{
    private val LOG = LogFactory.getLog(this.getClass)

    def getTypesMap(typeDictFile : File) : Map[String,List[DBpediaType]] = {
        LOG.info("Loading types map...")
        if (!(typeDictFile.getName.toLowerCase endsWith ".tsv"))
            throw new IllegalArgumentException("types mapping only accepted in tsv format so far! can't parse "+typeDictFile)
        // CAUTION: this assumes that the most specific type is listed last
        var typesMap = Map[String,List[DBpediaType]]()
        for (line <- Source.fromFile(typeDictFile, "UTF-8").getLines) {
            val elements = line.split("\t")
            val uri = elements(0)
            val t = new DBpediaType(elements(1))
            val typesList : List[DBpediaType] = typesMap.get(uri).getOrElse(List[DBpediaType]()) ::: List(t)
            typesMap = typesMap.updated(uri, typesList)
        }
        LOG.info("Done.")
        typesMap
    }

    def getTypesMap_java(typeDictFile : File) : java.util.Map[String,java.util.LinkedHashSet[DBpediaType]] = {
        LOG.info("Loading types map...")
        if (!(typeDictFile.getName.toLowerCase endsWith ".tsv"))
            throw new IllegalArgumentException("types mapping only accepted in tsv format so far! can't parse "+typeDictFile)
        // CAUTION: this assumes that the most specific type is listed last
        var typesMap = Map[String,java.util.LinkedHashSet[DBpediaType]]()
        for (line <- Source.fromFile(typeDictFile, "UTF-8").getLines) {
            val elements = line.split("\t")
            val uri = elements(0)
            val t = new DBpediaType(elements(1))
            val typesList : java.util.LinkedHashSet[DBpediaType] = typesMap.get(uri).getOrElse(new LinkedHashSet[DBpediaType]())
            typesList.add(t)
            typesMap = typesMap.updated(uri, typesList)
        }
        LOG.info("Done.")
        typesMap
    }
    
}