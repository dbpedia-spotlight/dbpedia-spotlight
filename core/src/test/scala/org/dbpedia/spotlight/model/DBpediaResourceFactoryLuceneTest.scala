package org.dbpedia.spotlight.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.dbpedia.spotlight.lucene.search.BaseSearcher
import org.dbpedia.spotlight.lucene.LuceneManager

import java.io.File
import org.apache.lucene.index.IndexReader
import org.apache.lucene.document.Document
//import org.dbpedia.spotlight.util.IndexingConfiguration
import org.apache.lucene.store.FSDirectory

/**
 * Test the creation of DBpediaResources using the DBpediaResourceFactoryLucene class.
 *
 * @author Alexandre Cançado Cardoso - accardoso
 */

@RunWith(classOf[JUnitRunner])
class DBpediaResourceFactoryLuceneTest extends FlatSpec with ShouldMatchers {

  /* Test Params */
  //Using a mock lucene index with only one uri ("Berlin"). Which can be found at: https://github.com/accardoso/test-files-spotlight/tree/master/lucene-index/index-berlin
  //The mock index path when the working directory is dbpedia-spotlight/
  var indexPath: String = "core"+File.separator+"src"+File.separator+"test"+File.separator+"scala"+File.separator+"org"+File.separator+"dbpedia"+File.separator+"spotlight"+File.separator+"model"+File.separator+"DBpediaResourceFactoryLuceneTest_mock"+File.separator+"index-berlin"
  //The mock index path when the working directory is dbpedia-spotlight/core/
  val alternativeIndexPath: String = "src"+File.separator+"test"+File.separator+"scala"+File.separator+"org"+File.separator+"dbpedia"+File.separator+"spotlight"+File.separator+"model"+File.separator+"DBpediaResourceFactoryLuceneTest_mock"+File.separator+"index-berlin"

  val indexDir: File = new File(indexPath)
  if(!indexDir.exists() || !indexDir.isDirectory)
    indexPath = alternativeIndexPath


  /* Tests Initialization */
  val indexDirectory = LuceneManager.pickDirectory(new File(indexPath))
  val lucene: LuceneManager = new LuceneManager(indexDirectory)
  val dbpediaResourceFactoryLucene: DBpediaResourceFactoryLucene = (new DBpediaResourceFactoryLucene(lucene, new BaseSearcher(lucene)))


  /* Tests for when the dbpedia id is informed */
  "DBpediaResourceFactoryLucene.from(uri)" should "construct a correct DBpediaResource for 'Berlin'" in { //In the current tested class the uri must be a dbpedia id (aka dbpedia uri)
    val uri: String = "Berlin"
    dbpediaResourceFactoryLucene.from(uri).getFullUri should be === new DBpediaResource(uri).getFullUri
  }

  it should "construct a correct DBpediaResource for 'http://dbpedia.org/resource/Berlin'" in {
    val uri: String = "http://dbpedia.org/resource/Berlin"
    dbpediaResourceFactoryLucene.from(uri).getFullUri should be === new DBpediaResource(uri).getFullUri
  }

//By the designers idea the class should pass in this test too. But requested feature to do it is not implemented yet.
//
//  it should "construct a correct DBpediaResource for 'http://en.wikipedia.org/resource/Berlin'" in {
//    val uri: String = "http://en.wikipedia.org/resource/Berlin"
//    dbpediaResourceFactoryLucene.from(uri).getFullUri should be === new DBpediaResource(uri).getFullUri
//  }
//
//  it should "construct a correct DBpediaResource for 'http://pt.wikipedia.org/resource/Berlim'" in {
//    val uri: String = "http://pt.wikipedia.org/resource/Berlim"
//    dbpediaResourceFactoryLucene.from(uri).getFullUri should be === new DBpediaResource(uri).getFullUri
//  }
//
//  it should "construct a correct DBpediaResource for 'http://en.wikipedia.org/resource/Berlim'" in {
//    val uri: String = "http://en.wikipedia.org/resource/Berlim"
//    dbpediaResourceFactoryLucene.from(uri).getFullUri should be === new DBpediaResource(uri).getFullUri
//  }
//
//  it should "construct a correct DBpediaResource for 'http://ontologia.organization.com/resource/Berlim'" in {
//    val uri: String = "http://ontologia.globo.com/resource/Berlim"
//    dbpediaResourceFactoryLucene.from(uri).getFullUri should be === new DBpediaResource(uri).getFullUri
//  }

  /* Test for when the lucene index document id is informed */
  "DBpediaResourceFactoryLucene.from(luceneDocId)" should "construct a correct DBpediaResource 'Berlin' for the 1st document of the Mock Lucene index" in {
    val luceneDocId: Int = 0
    val expectedResourceUri: String = "Berlin"
    dbpediaResourceFactoryLucene.from(luceneDocId).getFullUri should be === new DBpediaResource(expectedResourceUri).getFullUri
  }

}