package org.dbpedia.spotlight.io

import java.io._
import java.util.zip.{GZIPInputStream}
import io.Source
import scala.collection.mutable._
import org.dbpedia.spotlight.model._

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 6/6/12
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */

object FileOccsCategoriesSource
{
  /**
   * Creates an occ text to categories iterator from the occs  and articles_categories files
   */
  def fromFile(occTsvFile : File, articleCategoriesFile : File) : Traversable[(DBpediaResourceOccurrence,Set[DBpediaCategory])] = new FileOccurrenceCategorySource(occTsvFile,articleCategoriesFile)

  /**
   * DBpediaResourceOccurrence Source from previously saved data and dbpedia article_categories file.
   */
  private class FileOccurrenceCategorySource(tsvFile : File, articleCategoriesFile : File) extends Traversable[(DBpediaResourceOccurrence,Set[DBpediaCategory])] {

    override def foreach[U](f : ((DBpediaResourceOccurrence,Set[DBpediaCategory])) => U)  = {
      val occSource = FileOccurrenceSource.fromFile(tsvFile)
      var newArticle = ""
      var oldArticle = ""
      var artCatLine = Array[String]()
      var categories = Set[DBpediaCategory]()

      var articleCatLineIterator : Iterator[String] = Iterator.empty
      try {
        articleCatLineIterator = Source.fromFile(articleCategoriesFile, "UTF-8").getLines
      }
      catch {
        case e: java.nio.charset.MalformedInputException => articleCatLineIterator = Source.fromFile(articleCategoriesFile).getLines
      }

      occSource.foreach( dbpediaResOcc => {

        if (!dbpediaResOcc.resource.getFullUri.equals(oldArticle)) {
          if (!articleCatLineIterator.hasNext)
            (null,null)

          categories = Set[DBpediaCategory]()

          while (dbpediaResOcc.resource.getFullUri > newArticle && articleCatLineIterator.hasNext) {
            artCatLine = articleCatLineIterator.next().split(" ")
            newArticle = artCatLine(0).substring(1,artCatLine(0).length-1)
          }
          oldArticle = newArticle

          while (dbpediaResOcc.resource.getFullUri.equals(newArticle)&&articleCatLineIterator.hasNext) {
            categories += ( new DBpediaCategory(artCatLine(2).substring(1,artCatLine(2).length-1)) )
            artCatLine = articleCatLineIterator.next().split(" ")
            newArticle = artCatLine(0).substring(1,artCatLine(0).length-1)
          }
        }

        f((dbpediaResOcc, categories))
      })

    }
  }
}
