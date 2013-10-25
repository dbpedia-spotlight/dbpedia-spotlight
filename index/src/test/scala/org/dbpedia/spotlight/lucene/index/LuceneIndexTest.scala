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
import org.dbpedia.spotlight.util.IndexingConfiguration
import org.dbpedia.spotlight.model.SurfaceForm
import org.apache.lucene.store.FSDirectory

/**
 * This ScalaTest test if the lucene index is valid and has a minimum of documents with the field TYPE.
 *
 * @author Alexandre Cançado Cardoso - accardoso
 */


@RunWith(classOf[JUnitRunner])
class LuceneIndexTest extends FlatSpec with ShouldMatchers {

  /* Test params */
  //The path of the server.properties file
  val indexingConfigFileName: String = "../Useful/server.properties"
  //val indexingConfigFileName: String = "./conf/server.properties"
  //Minimum percentage of documents with TYPE to succeed at the second test
  val minPercentage: Int = 80


  /* Lucene Index Tests */

  "The Lucene Index" should "be valid" in {
    LuceneIndexTest.isIndexValid(indexingConfigFileName) should be === true
  }

  it should "has at least %d%% of valid documents with TYPE not empty".format(minPercentage) in {
    if (minPercentage < 0 && minPercentage > 100) //The minPercentage must belong to [0 ; 100] , a integer percentage
      throw new IllegalArgumentException("This test threshold must be a integer between (0,100]")

    LuceneIndexTest.totalOfValidDocs should be > 0 //If the Index has 0 valid documents it fail (and in this way we prevent division by 0 too)
    val percentage = LuceneIndexTest.numberOfDocsWithType * 100 / LuceneIndexTest.totalOfValidDocs
    SpotlightLog.debug(this.getClass, "Percentage of valid documents with TYPE = %d", percentage)
    percentage should be >= minPercentage //"should be" do demand Int
  }

  "Each valid Documents of the Lucene Index" should "have uri from which is possible to determinate and access the surface" in {
    //Note: if the index has 0 valid documents than this test succeed
    LuceneIndexTest.accessAllSurfaceForms should be === true
  }

}

object LuceneIndexTest {

  var numberOfDocsWithType: Int = 0 //number of Docs that has no empty type
  var totalOfValidDocs: Int = 0 //the total number of Docs

  var accessAllSurfaceForms = true

  var indexDirectory: FSDirectory = null

  def isIndexValid(indexingConfigFileName: String): Boolean = {
    //Use IndexingConfiguration class to get the index directory path
    val config = new IndexingConfiguration(indexingConfigFileName) // Demand that the "org.dbpedia.spotlight.data.stopWords.<language>" is setted with a valid path to the stop words file, which must call "stopwords.<language_in_short>.list"
    var indexPath = config.get("org.dbpedia.spotlight.candidateMap.dir") // Where the index path is informed at server.properties

    //Use LuceneManager and IndexReader to get and convert to a friendly format the index documents
    indexDirectory = LuceneManager.pickDirectory(new File(indexPath))
    val reader: IndexReader = IndexReader.open(indexDirectory)

    for (i <-0 to reader.maxDoc()-1) {
      if (!reader.isDeleted(i)){
        SpotlightLog.debug(this.getClass, "**** Running validation on Document #%d ****", i)
        if(!isDocumentValid(reader.document(i)))
          return false
      }
    }
    reader.close()

    true
  }

  private def isDocumentValid(doc: Document): Boolean = {
    val fields = doc.getFields

    val uriField: String = doc.get("URI")
    val uriCountField: String = doc.get("URI_COUNT")
    val typeFields = doc.getValues("TYPE").toList
    val surfaceFormStr: String = uriField.replaceAll("_", " ")
    val lucene: LuceneManager = new LuceneManager(indexDirectory)
    val surfaceFormField = lucene.getField(new SurfaceForm(surfaceFormStr))

    SpotlightLog.debug(this.getClass, "Document Fields: %s\n" +
      "URI field: %s\n" +
      "URI_COUNT field: %s\n" +
      "TYPE fields: %s\n" +
      "SURFACE_FORM: %s\n" +
      "SURFACE_FORM_FIELD: %s\n"
      , fields, uriField, uriCountField, typeFields, surfaceFormStr, surfaceFormField.toString)

    if(uriField == "" || uriCountField == "")
      return false

    if(typeFields.length >= 1)
      numberOfDocsWithType += 1

    totalOfValidDocs += 1

    //Of course the uriField is not empty so it should recover and access the surface form
    if(surfaceFormStr == "" || surfaceFormField == null || !surfaceFormField.isStored || !surfaceFormField.isIndexed)
      accessAllSurfaceForms = false

    true
  }
}