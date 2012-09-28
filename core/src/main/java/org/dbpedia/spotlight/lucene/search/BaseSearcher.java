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
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.exceptions.TimeoutException;
import org.dbpedia.spotlight.lucene.LuceneFeatureVector;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.model.vsm.FeatureVector;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * This class manages an index from surface form to candidate resources (surrogates)
 * @author pablomendes
 */
public class BaseSearcher implements Closeable {

    protected final Log LOG = LogFactory.getLog(getClass());

    LuceneManager mLucene;
    IndexSearcher mSearcher;
    public IndexReader mReader;

    //TODO create method that iterates over all documents in the index and computes this. (if takes too long, think about storing somewhere at indexing time)
    private double mNumberOfOccurrences = 69772256;

    protected BaseSearcher() {}  // allow subclasses to rewrite the super below

    public long objectCreationTime = 0;

    public BaseSearcher(LuceneManager lucene) throws IOException {
        this(lucene,false);
    }

    public BaseSearcher(LuceneManager lucene, boolean inMemory) throws IOException {
        this.mLucene = lucene;
        if (inMemory) {
            LOG.info("Creating in-memory lucene searcher... (may take 2-3 minutes and several GB of RAM)");
            this.mLucene.mContextIndexDir = new RAMDirectory(this.mLucene.mContextIndexDir);
        }
        LOG.info("Using index at: "+this.mLucene.mContextIndexDir);
        this.mReader = LuceneManager.openIndexReader(this.mLucene.mContextIndexDir); //will open single or multireader
        this.mSearcher = new IndexSearcher(this.mReader);
        LOG.debug("Done.");

        //If the DBpediaResourceFactory uses SQL, the mapping from Lucene docID to URI must be cached:
        if(mLucene.getDBpediaResourceFactory() instanceof DBpediaResourceFactorySQL) {
            /*
               Here, the Cache for the URIs is called (but not read) for the first time and will be
               created from the index and held within the Lucene cache manager.
            */
            LOG.debug("Caching all URIs" );
            FieldCache.DEFAULT.getStrings(mReader, LuceneManager.DBpediaResourceField.URI.toString());
            LOG.debug("Done.");
        }
        
    }

    public int getNumberOfEntries() {
        return this.mReader.numDocs();    // can use maxDoc?
    }

    public double getNumberOfOccurrences() {
        return this.mNumberOfOccurrences;
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

    public boolean isDeleted(int docNo) {
        return mReader.isDeleted(docNo);
    }

    public Document getFullDocument(int docNo) throws SearchException {
        Document document;
        try {
            document = mReader.document(docNo);
        } catch (IOException e) {
            throw new SearchException("Error reading document "+docNo,e);
        }
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

    public List<Document> getDocuments(DBpediaResource res, FieldSelector fieldSelector) throws SearchException {
        //LOG.trace("Retrieving documents for resource: "+res);

        // search index for surface form
        List<Document> documents = new ArrayList<Document>();

        // Iterate through the results:
        for (ScoreDoc hit : getHits(mLucene.getQuery(res))) {
            documents.add(getDocument(hit.doc, fieldSelector));
        }
        //LOG.debug(documents.size()+" documents found.");

        // return set of surrogates
        return documents;
    }

    /**
     * Basic search method used by all searches to the index.
     * @param query
     * @param n
     * @return
     * @throws SearchException
     */
    public ScoreDoc[] getHits(Query query, int n, int timeout, Filter filter) throws SearchException {
        ScoreDoc[] hits = null;
        try {
            //LOG.debug("Start search. timeout="+timeout);
            long start = System.nanoTime();
            TopScoreDocCollector collector = TopScoreDocCollector.create(n, false);
            //TimeLimitingCollector collector = new TimeLimitingCollector(tCollector, timeout);  //TODO try to bring this back later
            mSearcher.search(query, filter, collector);
            //mSearcher.
            hits = collector.topDocs().scoreDocs;
            long end = System.nanoTime();
            LOG.debug(String.format("Done search in %f ms. hits.length=%d",(end-start) / 1000000.0, hits.length));
        } catch (TimeLimitingCollector.TimeExceededException timedOutException) {
            throw new TimeoutException("Timeout (>"+timeout+"ms searching for surface form "+query.toString(),timedOutException);
        } catch (Exception e) {
            throw new SearchException("Error searching for surface form "+query.toString(),e);
        }
        //LOG.debug(hits.length+" hits found.");
        return hits;
    }

    /**
     * Search method with default filter
     * @param query
     * @param n number of results to return
     * @param timeout number of miliseconds before giving up this query
     * @return array of document id,score
     * @throws SearchException
     */
    public ScoreDoc[] getHits(Query query, int n, int timeout) throws SearchException {
        Filter filter = null; //TODO surfaceForm filter here?
        return getHits(query, n, timeout, filter);
    }

    // Uses default timeout
    public ScoreDoc[] getHits(Query query, int n) throws SearchException {
       return getHits(query, n, 5000);
    }

    // Uses default maxHits and timeout
    public ScoreDoc[] getHits(Query query) throws SearchException {
        return getHits(query, mLucene.topResultsLimit());
    }

    /**
     * Retrieves all DBpedia Resources in the index that are within a set of allowed URIs and match the input text.
     * Uses Lucene's MoreLikeThis rather than ICF as defined in DBpedia Spotlight's paper.
     * It is faster, but doesn't take into account selectional preferences of words wrt resources.
     *
     * @param context text containing a URI mention
     * @param resources allowed URIs
     * @return
     */
    public ScoreDoc[] getHits(Text context, Set<DBpediaResource> resources) throws SearchException {
        try {
            return getHits(mLucene.getQuery(context, resources, this.mReader));
        } catch (IOException e) {
            throw new SearchException("Error while executing query. ",e);
        }
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
     * Creates a DBpediaResource with all fields stored in the index
     * @deprecated  Use getDBpediaResource(int docNo, String[] fieldsToLoad)
     * TODO REMOVE. This is bad because the fields being loaded are opaque to the caller. Some need only URI, others need everything.
     * @param docNo
     * @return
     */
    public DBpediaResource getDBpediaResource(int docNo) throws SearchException {
        DBpediaResource r = null;
        String method = "";
        long start = System.nanoTime();
        DBpediaResourceFactory f = mLucene.getDBpediaResourceFactory();
        if (f==null) {
            method = "lucene";
            String[] fields = {LuceneManager.DBpediaResourceField.TYPE.toString(),
                    LuceneManager.DBpediaResourceField.URI.toString(),
                    LuceneManager.DBpediaResourceField.URI_COUNT.toString()
            };
            r = getDBpediaResource(docNo,fields); // load all available info from Lucene
        } else {
           method = "database";
           //String[] fields = {LuceneManager.DBpediaResourceField.URI.toString()};
           //r = getDBpediaResource(docNo,fields);   // load only URI from Lucene
           r = getCachedDBpediaResource(docNo);   // load URI from Lucene's Cache
           r = mLucene.getDBpediaResourceFactory().from(r.uri()); // load the rest of the info from DB
        }
        long end = System.nanoTime();
        //LOG.debug(String.format("DBpediaResource (%s) creation with %s took %f ms.", r.uri(), method, (end-start) / 1000000.0) );
        objectCreationTime += (end-start);

        return r;
    }

    /**
     * This is an experimental function to evaluate the feasibility of caching all URIs in Lucene
     * @param docNo
     * @return
     * @throws SearchException
     */
    public DBpediaResource getCachedDBpediaResource(int docNo) throws SearchException {
        try {
            String[] uris = FieldCache.DEFAULT.getStrings(mReader, LuceneManager.DBpediaResourceField.URI.toString());
            return new DBpediaResource(uris[docNo]);
        } catch (IOException e) {
            throw new SearchException("Error getting cached DBpediaResource.",e);
        }
    }




    /**
     * CONTEXT SEARCHER and SURROGATE SEARCHER
     * Loads only a few fields (faster)
     * TODO FACTORY move to Factory
     * @param docNo
     * @return
     * @throws SearchException
     */
    public DBpediaResource getDBpediaResource(int docNo, String[] fieldsToLoad) throws SearchException {

        FieldSelector fieldSelector = new MapFieldSelector(fieldsToLoad);
        Document document = getDocument(docNo, fieldSelector);
        Field uriField = document.getField(LuceneManager.DBpediaResourceField.URI.toString());
        if (uriField==null)
            throw new SearchException("Cannot find URI for document "+document);

        String uri = uriField.stringValue();
        if (uri==null)
            throw new SearchException("Cannot find URI for document "+document);

        DBpediaResource resource = new DBpediaResource(uri);

        for (String fieldName: fieldsToLoad) {
            Field field = document.getField(fieldName);
            if (field != null) Factory.setField(resource, LuceneManager.DBpediaResourceField.valueOf(fieldName), document);
        }
        if (resource.prior() == 0.0) { // adjust prior
            resource.setPrior(resource.support() / this.getNumberOfOccurrences());
        }
        return resource;
    }

//        // Returns the first URI that can be found in the document number docNo   (old, superceded by Factory.setField)
//    //TODO move to Factory
//    //TODO why is this overriding BaseSearcher? can merge?
//    public DBpediaResource getDBpediaResource(int docNo) throws SearchException {
//
//        FieldSelector fieldSelector = new MapFieldSelector(onlyUriAndTypes);
//
//        LOG.trace("Getting document number " + docNo + "...");
//        Document document = createDocument(docNo, fieldSelector);
//        String uri = document.get(LuceneManager.DBpediaResourceField.URI.toString());
//        if (uri==null)
//            throw new SearchException("Cannot find URI for document "+document);
//
//        LOG.trace("Setting URI, types and support...");
//        DBpediaResource resource = new DBpediaResource(uri);
//        resource.setTypes( getDBpediaTypes(document) );
//        resource.setSupport( getSupport(document) ); //TODO this can be optimized for time performance by adding a support field. (search for the most likely URI then becomes a bit more complicated)
//
//        //LOG.debug("uri:"+uri);
//        return resource;
//    }

    public SurfaceForm getSurfaceForm(int docNo) throws SearchException {
        String[] onlyUriAndTypes = {LuceneManager.DBpediaResourceField.SURFACE_FORM.toString()};
        FieldSelector fieldSelector = new MapFieldSelector(onlyUriAndTypes);
        Document document = getDocument(docNo,fieldSelector);
        Field sfField = document.getField(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString());
        if (sfField==null)
            throw new SearchException("Cannot find SurfaceForm for document "+document);

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
            int frequency = mReader.docFreq(term); // DF
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
     *
     * TODO warm up property with surface form + context
     *      Currently this only gets the most common terms (which are all probably from CONTEXT field, so not much gain is seen for the ICF
     *      Best would be to get a list of X most common surface forms and execute a query with that surface form and the top Y most common context terms
     * @param n
     * @return
     * @throws IOException
     */
    public void warmUp(int n) {

        try {
            long start = System.nanoTime();
            List<Map.Entry<Term,Integer>> terms = getTopTerms(mReader);
            LOG.info(String.format("Index has %s terms. Will warm up cache with the %s top terms.", terms.size(), n));

            for (int i=0; i<n; i++) {
                Term t = terms.get(i).getKey();
                //TODO For a second-level cache warmUp we need surface forms and context together, but this is app dependent
//                String[] commonSurfaceForms = {"*"}; //get a list of surface forms from somewhere
//                for (String sf: commonSurfaceForms) {
//                    getHits(new CandidateResourceQuery(new Term(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString(), sf), t));
//                }

                //int count = terms.get(i).getValue();
                //LOG.trace(String.format("Text: %s, Count: %s", t.text(), count));

                getHits(new TermQuery(t), 3, 1000); // warm up first-level cache (lucene's own)

            }


            String time = new Long( (System.nanoTime() - start) / 1000000000 ).toString();
            //Files.write(Joiner.on("\n").join(terms), new File("/home/pablo/workspace/dbpa/trunk/src/web/topterms.tsv"), Charset.defaultCharset()); //TODO use one charset consistently throughout
            LOG.info(String.format("Warm up took %s ms.",time));
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
