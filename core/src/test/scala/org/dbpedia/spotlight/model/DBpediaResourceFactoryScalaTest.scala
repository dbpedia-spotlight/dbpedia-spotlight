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
 *
 * @author Alexandre Cançado Cardoso - accardoso
 */

@RunWith(classOf[JUnitRunner])
class DBpediaResourceFactoryScalaTest extends FlatSpec with ShouldMatchers {

  /* Test Params */

  val indexPath: String = "/media/221CF5031CF4D2B1/Users/Alexandre/Intrisic_SpotIndex/index-withSF-withTypes-compressed"

  /* Tests */

  val indexDirectory = LuceneManager.pickDirectory(new File(indexPath))
  val lucene: LuceneManager = new LuceneManager(indexDirectory)
  val dbpediaResourceFactoryLucene: DBpediaResourceFactoryLucene = (new DBpediaResourceFactoryLucene(lucene, new BaseSearcher(lucene)))

  "DBpediaResource Factory" should "construct a correct DBpediaResource for 'Berlin'" in {
    val uri: String = "Berlin"
    dbpediaResourceFactoryLucene.from(uri).getFullUri should be === new DBpediaResource(uri).getFullUri
  }

  it should "construct a correct DBpediaResource for 'http://dbpedia.org/resource/Berlin'" in {
    val uri: String = "http://dbpedia.org/resource/Berlin"
    dbpediaResourceFactoryLucene.from(uri).getFullUri should be === new DBpediaResource(uri).getFullUri
  }

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
//  it should "construct a correct DBpediaResource for 'http://ontologia.globo.com/resource/Berlim'" in {
//    val uri: String = "http://ontologia.globo.com/resource/Berlim"
//    dbpediaResourceFactoryLucene.from(uri).getFullUri should be === new DBpediaResource(uri).getFullUri
//  }

}