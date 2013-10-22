package org.dbpedia.spotlight.lucene.index

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.dbpedia.spotlight.lucene.LuceneManager
import java.io.File
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.IndexSearcher


@RunWith(classOf[JUnitRunner])
class InformedIndexTest extends FlatSpec with ShouldMatchers {

  "The informed index" should " be valid" in {

    InformedIndexTest.isIndexValid() should be === true
  }

}

object InformedIndexTest {

  val indexPath: String = "/media/221CF5031CF4D2B1/Users/Alexandre/Intrisic_SpotIndex/index-withSF-withTypes-compressed"
  //TODO remove indexPath and get the index path or the already instantiated LuceneManager

  var lucene: LuceneManager = null

  def isIndexValid(): Boolean = {
    //MergedOccurrencesContextIndexer.get_mLucene

    // Instantiate a LuceneManager with the index directory
    val indexDirectory = LuceneManager.pickDirectory(new File(indexPath))
    //lucene = new LuceneManager(indexDirectory)
    val reader: IndexReader = IndexReader.open(indexDirectory)
    //val searcher: IndexSearcher = new IndexSearcher(reader)

//    var i = 0
//    var doc = searcher.doc(i)
    for (i <-0 to 5){//reader.maxDoc()) {
      if (!reader.isDeleted(i)){
        val doc = reader.document(i)
        //val docId: String = doc.get("docId")
        val fields = doc.getFields
         println(fields)

      }
    }



    reader.close()

    false
  }

// //Reference: https://github.com/kurzum/nif4oggd/blob/master/index/src/main/java/org/aksw/lucene/extractor/DocumentExtractor.java#L222
//  private List<Place> getPlaces(String cityFilter) throws IOException {
//
//    List<Place> result = new ArrayList<Place>();
//
//    LOG.debug("Reading streets by city...");
//    LOG.debug("City:%s".format(city));
//
//    IndexReader reader = IndexReader.open(FSDirectory.open(indexDirectory));
//    IndexSearcher searcher = new IndexSearcher(reader);
//
//    BooleanQuery bq = new BooleanQuery();
//    bq.add(new TermQuery(new Term(IndexField.CITY, cityFilter.toLowerCase())), BooleanClause.Occur.MUST);
//
//    ScoreDoc[] hits = searcher.search(bq, Integer.MAX_VALUE).scoreDocs;
//
//    for (int i = 0; i < hits.length; i++) {
//
//      Document doc = searcher.doc(hits[i].doc);
//
//      String street = doc.get(IndexField.DESCRIPTION).toLowerCase();
//      String city = doc.get(IndexField.CITY).toLowerCase();
//      Place p = new Place();
//      p.setName(street);
//      p.setCity(city);
//      result.add(p);
//
//    }
//
//    reader.close();
//
//    return result;
//
//  }

}