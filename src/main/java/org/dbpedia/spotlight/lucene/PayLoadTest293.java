package org.dbpedia.spotlight.lucene;

import junit.framework.TestCase;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilter;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.payloads.FloatEncoder;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.payloads.BoostingTermQuery;

import java.io.Reader;
import java.io.IOException;

/**
 *
 *
 **/
public class PayLoadTest293 extends TestCase {
Directory dir;

  public static String[] DOCS = {
          "The quick|2.0 red|2.0 fox|10.0 jumped|5.0 over the lazy|2.0 brown|2.0 dogs|10.0",
          "The quick red fox jumped over the lazy brown dogs",//no boosts
          "The quick|2.0 red|2.0 fox|10.0 jumped|5.0 over the old|2.0 brown|2.0 box|10.0",
          "Mary|10.0 had a little|2.0 lamb|10.0 whose fleece|10.0 was|5.0 white|2.0 as snow|10.0",
          "Mary had a little lamb whose fleece was white as snow",
          "Mary|10.0 takes on Wolf|10.0 Restoration|10.0 project|10.0 despite ties|10.0 to sheep|10.0 farming|10.0",
          "Mary|10.0 who lives|5.0 on a farm|10.0 is|5.0 happy|2.0 that she|10.0 takes|5.0 a walk|10.0 every day|10.0",
          "Moby|10.0 Dick|10.0 is|5.0 a story|10.0 of a whale|10.0 and a man|10.0 obsessed|10.0",
          "The robber|10.0 wore|5.0 a black|2.0 fleece|10.0 jacket|10.0 and a baseball|10.0 cap|10.0",
          "The English|10.0 Springer|10.0 Spaniel|10.0 is|5.0 the best|2.0 of all dogs|10.0"
  };
  protected PayloadSimilarity payloadSimilarity;

  @Override
  protected void setUp() throws Exception {
    dir = new RAMDirectory();

    PayloadEncoder encoder = new FloatEncoder();
    IndexWriter writer = new IndexWriter(dir, new PayloadAnalyzer(encoder), true, IndexWriter.MaxFieldLength.UNLIMITED);
    payloadSimilarity = new PayloadSimilarity();
    writer.setSimilarity(payloadSimilarity);
    for (int i = 0; i < DOCS.length; i++) {
      Document doc = new Document();
      Field id = new Field("id", "doc_" + i, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
      doc.add(id);
      //Store both position and offset information
      Field text = new Field("body", DOCS[i], Field.Store.NO, Field.Index.ANALYZED);
      doc.add(text);
      writer.addDocument(doc);
    }
    writer.close();
  }

  public void testPayloads() throws Exception {
    IndexSearcher searcher = new IndexSearcher(dir, true);
    searcher.setSimilarity(payloadSimilarity);//set the similarity.  Very important
    BoostingTermQuery btq = new BoostingTermQuery(new Term("body", "fox"));
    TopDocs topDocs = searcher.search(btq, 10);
    printResults(searcher, btq, topDocs);

    TermQuery tq = new TermQuery(new Term("body", "fox"));
    topDocs = searcher.search(tq, 10);
    printResults(searcher, tq, topDocs);
  }

  private void printResults(IndexSearcher searcher, Query query, TopDocs topDocs) throws IOException {
    System.out.println("-----------");
    System.out.println("Results for " + query + " of type: " + query.getClass().getName());
    for (int i = 0; i < topDocs.scoreDocs.length; i++) {
      ScoreDoc doc = topDocs.scoreDocs[i];
      System.out.println("Doc: " + doc.toString());
      System.out.println("Explain: " + searcher.explain(query, doc.doc));
    }
  }

  class PayloadSimilarity extends DefaultSimilarity {
    @Override
    public float scorePayload(String fieldName, byte[] bytes, int offset, int length) {
      return PayloadHelper.decodeFloat(bytes, offset);//we can ignore length here, because we know it is encoded as 4 bytes
    }
  }

  class PayloadAnalyzer extends Analyzer {
    private PayloadEncoder encoder;

    PayloadAnalyzer(PayloadEncoder encoder) {
      this.encoder = encoder;
    }

    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new WhitespaceTokenizer(reader);
      result = new LowerCaseFilter(result);
      result = new DelimitedPayloadTokenFilter(result, '|', encoder);
      return result;
    }
  }
}