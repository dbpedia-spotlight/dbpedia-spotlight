/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.lucene.search;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.*;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.LuceneFeatureVector;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.model.vsm.FeatureVector;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * This class manages an index from surface form to candidate resources (surrogates)
 * User: PabloMendes
 * Date: Jun 24, 2010
 * Time: 12:37:21 PM
 */
public class BaseSearcher implements Closeable {

    final Log LOG = LogFactory.getLog(getClass());

    LuceneManager mLucene;
    IndexSearcher mSearcher;
    IndexReader mReader;

    public BaseSearcher(LuceneManager lucene) throws IOException {
        this.mLucene = lucene;
        LOG.debug("Opening IndexSearcher and IndexReader for Lucene directory "+this.mLucene.mContextIndexDir+" ...");
        this.mReader = IndexReader.open(this.mLucene.mContextIndexDir, true); // read-only=true
        this.mSearcher = new IndexSearcher(this.mReader);
        LOG.debug("Done.");
    }

    public long getNumberOfEntries() {
        return this.mReader.maxDoc();
    }

    /**
     * Generic search method that will be used by other public methods in this class
     * @param query
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected List<FeatureVector> search(Query query) throws SearchException {
        // List of results
        List<FeatureVector> surrogates = new ArrayList<FeatureVector>();

        // Iterate through the results:
        for (ScoreDoc hit : getHits(query)) {
            DBpediaResource resource = getDBpediaResource(hit.doc);
            TermFreqVector vector = getVector(hit.doc);
            surrogates.add(new LuceneFeatureVector(resource, vector));
        }

        //LOG.debug(surrogates.size()+" hits found.");
        return surrogates;
    }

    public Document getFullDocument(int docNo) throws SearchException {
        Document document;
        try {
            document = mReader.document(docNo);
            //document = mReader.document(docNo, fieldSelector);
        } catch (IOException e) {
            throw new SearchException("Error reading document "+docNo,e);
        }
        //LOG.debug("docNo:"+docNo);
        return document;
    }

    public Document getDocument(int docNo, FieldSelector selector) throws SearchException {
        Document document;
        try {
            document = mReader.document(docNo, selector);
            //document = mReader.document(docNo, fieldSelector);
        } catch (IOException e) {
            throw new SearchException("Error reading document "+docNo,e);
        }
        //LOG.debug("docNo:"+docNo);
        return document;
    }

    public ScoreDoc[] getHits(Query query, int n) throws SearchException {

        ScoreDoc[] hits;
        try {
            hits = mSearcher.search(query, null, n).scoreDocs;
        } catch (IOException e) {
            throw new SearchException("Error searching for surface form "+query.toString(),e);
        }
        //LOG.debug(hits.length+" hits found.");
        return hits;
    }

    public ScoreDoc[] getHits(Query query) throws SearchException {
        return getHits(query, mLucene.topResultsLimit());
    }

    public ScoreDoc[] getHits(Set<DBpediaResource> resources, Text context) throws SearchException {
        return getHits(mLucene.getQuery(resources, context));
    }

    public TermFreqVector getVector(int docNo) throws SearchException {
        TermFreqVector vector = null;
        try {
            vector = mReader.getTermFreqVector(docNo, LuceneManager.DBpediaResourceField.CONTEXT.toString());
            if (vector==null)
                throw new IllegalStateException("TermFreqVector for document "+docNo+" is null.");
        } catch (IOException e) {
            throw new SearchException("Error reading TermFreqVector for surrogate "+docNo,e);
        }
        //LOG.debug("vector:"+vector);
        return vector;
    }

    @Override
    public void close() throws IOException {
        //LOG.debug("Closing searcher.");
        mSearcher.close();
        //mReader.close();
    }

    /**
     * CONTEXT SEARCHER and SURROGATE SEARCHER
     * @param docNo
     * @return
     * @throws SearchException
     */
    public DBpediaResource getDBpediaResource(int docNo) throws SearchException {

        String[] onlyUriAndTypes = {LuceneManager.DBpediaResourceField.URI.toString()};
        FieldSelector fieldSelector = new MapFieldSelector(onlyUriAndTypes);
        Document document = getDocument(docNo, fieldSelector);
        Field uriField = document.getField(LuceneManager.DBpediaResourceField.URI.toString());
        if (uriField==null)
            throw new SearchException("Cannot find URI for document "+document);

        String uri = uriField.stringValue();
        if (uri==null)
            throw new SearchException("Cannot find URI for document "+document);

        //LOG.debug("uri:"+uri);
        return new DBpediaResource(uri);
    }

    public SurfaceForm getSurfaceForm(int docNo) throws SearchException {
        String[] onlyUriAndTypes = {LuceneManager.DBpediaResourceField.SURFACE_FORM.toString()};
        FieldSelector fieldSelector = new MapFieldSelector(onlyUriAndTypes);
        Document document = getDocument(docNo,fieldSelector);
        Field sfField = document.getField(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString());
        if (sfField==null)
            throw new SearchException("Cannot find URI for document "+document);

        String sf = sfField.stringValue();
        if (sf==null)
            throw new SearchException("Cannot find URI for document "+document);

        //LOG.debug("uri:"+uri);
        return new SurfaceForm(sf);
    }

    public boolean isContainedInIndex(SurfaceForm sf) {
        try {
            if (getHits(mLucene.getQuery(sf), 1).length > 0)
                return true;
        } catch (SearchException e) {
            LOG.info("SearchException in isContainedInIndex("+sf+"): "+e);
        }
        return false;
    }

//    public Document termDocs(DBpediaResource resource) throws IOException {
//
//        Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(),resource.uri());
//        mReader.terms(uriTerm);
//        TermDocs t = mReader.termDocs();
//
//        return null;
//    }


  /**
   * Computes a term frequency map for the index at the specified location.
   * @param 
   * @return a Boolean OR query.
   * @throws Exception if one is thrown.
     * @author sujitpal (computeTopTermQuery in http://sujitpal.blogspot.com/2009/02/summarization-with-lucene.html)
     * @author pablomendes adapted from sujitpal
   */
    public static List<Map.Entry<Term,Integer>> getTopTerms(IndexReader mReader) throws IOException {

        final Map<Term,Integer> frequencyMap = new HashMap<Term,Integer>();

        TermEnum terms = mReader.terms(); //TODO check what can we do about fields here. should have only top terms for context field?
        while (terms.next()) {
            Term term = terms.term();
            int frequency = mReader.docFreq(term);
            frequencyMap.put(term, frequency);
        }

        // sort the term map by frequency descending
        Ordering descOrder = new Ordering<Map.Entry<Term,Integer>>() {
            public int compare(Map.Entry<Term,Integer> left, Map.Entry<Term,Integer> right) {
                return Ints.compare(right.getValue(), left.getValue());
            }
        };
        List<Map.Entry<Term,Integer>> sorted = descOrder.sortedCopy(frequencyMap.entrySet());

        return sorted;
    }

    /**
     * Warms up the index with the n most common terms.
     * @param n
     * @return
     * @throws IOException
     */
    public void warmUp(int n) {
        try {
            List<Map.Entry<Term,Integer>> terms = getTopTerms(mReader);
            LOG.info(String.format("Index has %s terms. Will warm up cache with the %s top terms.", terms.size(), n));
            for (int i=0; i<n; i++) {
                Term t = terms.get(i).getKey();
                //int count = terms.get(i).getValue();
                getHits(new TermQuery(t));
                //LOG.trace(String.format("Text: %s, Count: %s", t.text(), count));
            }

            //Files.write(Joiner.on("\n").join(terms), new File("/home/pablo/workspace/dbpa/trunk/src/web/topterms.tsv"), Charset.defaultCharset()); //TODO use one charset consistently throughout

        } catch (Exception e) {
            LOG.error("Error warming up the cache. Ignoring. "); //TODO Throw SetupException
            e.printStackTrace();
        }
    }

    /**
     * for testing QueryAutoStopWordsAnalyzer
     * @return
     */
    public IndexReader getIndexReader() {
        return mReader;
    }

}
