package org.dbpedia.spotlight.lucene.index

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.dbpedia.spotlight.lucene.LuceneManager
import java.io.File
import org.apache.lucene.index.IndexReader
import org.apache.lucene.document.Document
import org.dbpedia.spotlight.log.SpotlightLog

/**
 * This ScalaTest test if the informed text is valid.
 *
 * @author Alexandre Cançado Cardoso - accardoso
 */


@RunWith(classOf[JUnitRunner])
class InformedIndexTest extends FlatSpec with ShouldMatchers {

  "The informed index" should " be valid" in {
    InformedIndexTest.isIndexValid() should be === true
  }

}

object InformedIndexTest {

  val indexPath: String = "/media/221CF5031CF4D2B1/Users/Alexandre/Intrisic_SpotIndex/index-withSF-withTypes-compressed"
  //TODO remove indexPath and get the index path from who has got it form server.properties OR use a default path and set indexPath to it

  var lucene: LuceneManager = null

  def isIndexValid(): Boolean = {
    val indexDirectory = LuceneManager.pickDirectory(new File(indexPath))
    val reader: IndexReader = IndexReader.open(indexDirectory)

    for (i <-0 to reader.maxDoc()) {
    //for (i <-0 to 5) {
      if (!reader.isDeleted(i)){
        SpotlightLog.info(this.getClass, "**** Running validation on Document #%d ****", i)
        SpotlightLog.debug(this.getClass, "**** Running validation on Document #%d ****", i)
        if(!isDocumentValid(reader.document(i)))
          return false
      }
    }

    reader.close()
    true
  }

  def isDocumentValid(doc: Document): Boolean = {
    val fields = doc.getFields

    val uriField: String = doc.get("URI")
    val uriCountField: String = doc.get("URI_COUNT")
    val typeFields = doc.getValues("TYPE").toList

    SpotlightLog.debug(this.getClass, "Document Fields: %s\n" +
                      "URI field: %s\n" +
                      "URI_COUNT field: %s\n" +
                      "TYPE fields: %s\n", fields, uriField, uriCountField, typeFields)

//    SpotlightLog.info(this.getClass, "Document Fields: %s\n" +
//                       "URI field: %s\n" +
//                       "URI_COUNT field: %s\n" +
//                       "TYPE fields: %s\n", fields, uriField, uriCountField, typeFields)

    if(uriField == null || uriCountField == null)
      return false

    true
  }
}