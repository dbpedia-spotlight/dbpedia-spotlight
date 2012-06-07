package org.dbpedia.spotlight.db.io

import io.Source
import scala.Predef._
import org.dbpedia.spotlight.model._
import java.io.{File, FileInputStream, InputStream}
import org.dbpedia.spotlight.model.Factory.OntologyType
import collection.immutable.HashMap
import scala.collection.JavaConverters._
import java.util.NoSuchElementException


/**
 * @author Joachim Daiber
 *
 */

/**
 * Represents a source of DBpediaResources
 */

object DBpediaResourceSource {

  def fromTSVInputStream(
    conceptList: InputStream,
    counts: InputStream,
    instanceTypes: InputStream
  ): java.util.Map[DBpediaResource, Int] = {

    var id = 1
    val resourceMap: Map[String, DBpediaResource] = (Source.fromInputStream(conceptList).getLines() map {
      line: String => {
        val res = new DBpediaResource(line.trim)
        res.id = id
        id += 1
        Pair(line.trim, res)
      }
    }).asInstanceOf[Iterator[Pair[String, DBpediaResource]]].toMap

    //Read counts:
    Source.fromInputStream(counts).getLines() foreach {
      line: String => {
        val Array(id: String, count: String) = line.trim().split('\t')
        resourceMap(id).setSupport(count.toInt)
      }
    }

    //Read types:
    Source.fromInputStream(instanceTypes).getLines() foreach {
      line: String => {
        val Array(id: String, typeURI: String) = line.trim().split('\t')

        try {
          if (! typeURI.startsWith(SchemaOrgType.SCHEMAORG_PREFIX))
            resourceMap(id).types ::= OntologyType.fromURI(typeURI)
        } catch {
          case e: NoSuchElementException => System.err.println("ERROR: DBpedia resource %s does not exist in concept list, this is likely due to different versions used for concept list and types".format(id) )
        }
      }
    }

    resourceMap.iterator.map( f => Pair(f._2, f._2.support) ).toMap.asJava
  }


  def fromTSVFile(
    conceptList: File,
    counts: File,
    instanceTypes: File
  ): java.util.Map[DBpediaResource, Int] = fromTSVInputStream(
      new FileInputStream(conceptList),
      new FileInputStream(counts),
      new FileInputStream(instanceTypes)
    )

}
