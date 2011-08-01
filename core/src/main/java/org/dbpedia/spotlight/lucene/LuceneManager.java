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

package org.dbpedia.spotlight.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.search.CandidateResourceQuery;
import org.dbpedia.spotlight.util.MemUtil;
import org.dbpedia.spotlight.model.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class defines a policy for storing/searching in lucene.
 * It defines the correct behavior if you want to search for a Surface Form, DBpedia Resource or Context.
 * Whenever possible you should use the methods here instead of implementing your own lucene-specific code.
 *
 * Since the Analyzer and Similarity classes have to be the same for indexing and searching, they are kept here.
 * The case insensitivity behavior also should be used consistently across classes, so we keep it here as well.
 *
 * TODO Should be singleton to assure that reader and writer are using the same config?
 *
 * @author pablomendes
 */
public class LuceneManager {

    /*TODO yes, we know we shouldn't have public fields floating around.
    "Tips on Choosing an Access Level: (from http://download.oracle.com/docs/cd/E17409_01/javase/tutorial/java/javaOO/accesscontrol.html)
    If other programmers use your class, you want to ensure that errors from misuse cannot happen.
    Access levels can help you do this.
    Use the most restrictive access level that makes sense for a particular member.
    Use private unless you have a good reason not to."
    */

    // How to break down the input text (used in BaseIndexer when creating the IndexWriter and at query time inside getQuery)
    protected Analyzer mDefaultAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_29));

    // How to compare contexts
    private Similarity mContextSimilarity = new DefaultSimilarity();

    // How to access the index (Can be RAM based or in disk)
    public Directory mContextIndexDir;

    /* Determines ... buffering added documents and deletions before they are flushed to the Directory.
       NOTE: because IndexWriter uses ints when managing its internal storage, (...) it's best to set this value comfortably under 2048.
       http://lucene.apache.org/java/3_0_2/api/all/org/apache/lucene/index/IndexWriter.html#setRAMBufferSizeMB%28double%29
    */
    protected int RAMBufferSizeMB = Math.min(new Long(MemUtil.getFreeMemoryMB()).intValue(),
                                                1000) ;          // DEFAULT value (not configurable yet)

    // what to do if lucene.mContextIndexDir already exists (so we do not unvoluntary add to an existing index).
    // true to create the index or overwrite the existing one; false to append to the existing index
    public boolean shouldOverwrite = false;  //BE CAREFUL: indices will be overwritten if true !!!!!

    // this value specifies how many top results Lucene should return
    public int topResultsLimit = 100;


    public LuceneManager(Directory directory) throws IOException {
        this.mContextIndexDir = directory;
    }

    // this file contains a bunch of useful config info for searching indexes
//    protected SpotlightConfiguration mSearchConfiguration;
//
//    public LuceneManager(Directory directory, SpotlightConfiguration configuration) throws IOException {
//        this.mContextIndexDir = directory;
//        this.mSearchConfiguration = configuration;
//    }
//
//    public SpotlightConfiguration configuration() {
//        return mSearchConfiguration;
//    }

    //---- GETTERS ---- They don't follow Java's naming convention - getDirectory() -, but rather Scala's style - mContextIndexDir()
    
    public Directory directory() {
        return mContextIndexDir;
    }

    public static Directory pickDirectory(File indexDir) throws IOException {
        if (System.getProperty("os.name").equals("Linux")) {
            return new NIOFSDirectory(indexDir); 
        } else {
            return FSDirectory.open(indexDir);
        }
    }

    public Analyzer defaultAnalyzer() {
        return mDefaultAnalyzer;
    }

    public void setDefaultAnalyzer(Analyzer analyzer) {
        this.mDefaultAnalyzer = analyzer;
    }

    public void setContextSimilarity(Similarity contextSimilarity) {
        this.mContextSimilarity = contextSimilarity;
    }
    public Similarity contextSimilarity() {
        return mContextSimilarity;
    }

    public int RAMBufferSizeMB() {
        return RAMBufferSizeMB;
    }

    public boolean shouldOverride() {
        return shouldOverwrite;
    }

    public int topResultsLimit() {
        return topResultsLimit;
    }


    public enum DBpediaResourceField {
        URI("URI"), URI_COUNT("URI_COUNT"), URI_PRIOR("URI_PRIOR"), SURFACE_FORM("SURFACE_FORM"), CONTEXT("CONTEXT"), TYPE("TYPE");
        private String name;
        DBpediaResourceField(String name) {
            this.name = name;
        }
        public static String[] stringValues() {
            String[] stringValues = new String[values().length];
            int i = 0;
            for (DBpediaResourceField value: values()) {
                stringValues[i++] = value.toString();
            }
            return stringValues;
        }
        @Override
        public String toString() {
            return this.name;
        }
    }

    private int getUriCount(Document doc) { //TODO couldn't return backwards compatible count?
        Field uriCountField = doc.getField(DBpediaResourceField.URI_COUNT.toString());
        if(uriCountField == null) {
            return 0;
        }
        return Integer.parseInt(uriCountField.stringValue());
    }

    /**
     * Updates the URI_COUNT count of doc2 with the URI_COUNT of doc1.
     * @param doc1
     * @param doc2
     * @return doc2 with updated URI_COUNT field
     */
    private Document updateUriCount(Document doc1, Document doc2) {
        Field f = getUriCountField(0);
        f.setValue(Integer.toString(getUriCount(doc1)+getUriCount(doc2)));
        doc2.removeField(DBpediaResourceField.URI_COUNT.toString());
        doc2.add(f);
        return doc2;
    }

    /**
     * Index URIs only once. Aggregate URI_COUNT. Merge rest.
     */
    public Document merge(Document doc1, Document doc2) {
        for (DBpediaResourceField fieldName: DBpediaResourceField.values()) {
            if(fieldName == DBpediaResourceField.URI_COUNT) {
                doc2 = updateUriCount(doc1, doc2);
                continue;
            }
            Field[] fields = doc1.getFields(fieldName.toString());
            for(Field f : fields) {
                if (f != null) {
                    if(fieldName == DBpediaResourceField.URI && doc2.getField(fieldName.toString()) != null) {
                        break;
                    }
                    doc2.add(f);
                }
            }
        }
        return doc2;
    }

    public Document merge_multiUris(Document doc1, Document doc2) {
        //Document doc = new Document();
        for (DBpediaResourceField f: DBpediaResourceField.values()) {
            Field f1 = doc1.getField(f.toString());
            if (f1!=null)
                doc2.add(f1);
        }
        return doc2;
    }

    public Document add(Document doc, SurfaceForm sf) {
        Field sfField = getField(sf);
        doc.add(sfField);
        return doc;
    }

    public Document add(Document doc, Double prior) {

        Field priorField = doc.getField(LuceneManager.DBpediaResourceField.URI_PRIOR.toString());
        if (priorField==null) {
            priorField = getUriPriorField(prior);
        } else {
            priorField.setValue(prior.toString());
            doc.removeFields(LuceneManager.DBpediaResourceField.URI_PRIOR.toString());
        }
        doc.add(priorField);

        return doc;
    }

    public Document add(Document doc, DBpediaType t) {
        Field typeField = getField(t);
        doc.add(typeField);
        return doc;
    }

    /**
     * Modify a Document so it does not Field.Store certain fields anymore     
     */
    public Document unstore(Document doc, List<DBpediaResourceField> unstoreFields) {
        Document newDoc = new Document();

        for (DBpediaResourceField enumField : DBpediaResourceField.values()) {
            Field.Store store = Field.Store.YES;
            if (unstoreFields.contains(enumField)) {
                store = Field.Store.NO;
            }
            
            Field.Index index = Field.Index.NOT_ANALYZED_NO_NORMS;
            Field.TermVector termVector = Field.TermVector.NO;
            if (enumField.equals(DBpediaResourceField.CONTEXT)) {
                index = Field.Index.ANALYZED;
                termVector = Field.TermVector.YES;
            }
            else if(enumField.equals(DBpediaResourceField.SURFACE_FORM)) {
                index = Field.Index.NOT_ANALYZED;
            }

            String fieldString = enumField.toString();
            for (Field luceneField : doc.getFields(fieldString)) {
                String value = luceneField.stringValue();
                //if (luceneField.name().equals(DBpediaResourceField.SURFACE_FORM.toString())) value = luceneField.stringValue().toLowerCase(); //HACK temp. remove now.
                Field newField = new Field(fieldString,
                                           value,
                                           store,
                                           index,
                                           termVector);
                newDoc.add(newField);
            }
        }
        return newDoc;
    }



    /*---------- Basic methods for querying the index correctly ----------*/

//    public Query getAutoStopwordedQuery() {
//        QueryParser parser = new QueryParser(Version.LUCENE_29, DBpediaResourceField.CONTEXT.toString(), autoStopWordAnalyzer);
//    }

    public Query getMustQuery(Text context) throws SearchException {
        Set<Term> terms = new HashSet<Term>();
        Query orQuery = getQuery(context);
        orQuery.extractTerms(terms);
        System.out.println(String.format("Terms: %s",terms));
        //Set<Term> qTerms = new HashSet<Term>(); //DUPLICATED CODE?
        //orQuery.extractTerms(qTerms);
        //return getMustQuery(qTerms);
        return getMustQuery(terms);
    }


    public Query getMustQuery(Set<Term> qTerms) {
        BooleanQuery andQuery = new BooleanQuery();
        for (Term t: qTerms) {
            andQuery.add(new TermQuery(t), BooleanClause.Occur.MUST);
        }
        return andQuery;
    }

    /**
     * TODO Gives us a chance to add some smarter/faster analyzer for query time that, for example:
     * - automatically discards very common words when querying
     * - uses only words around a given surface form
     * @return
     */
    public Analyzer getQueryTimeContextAnalyzer() {
        //Analyzer queryTimeAnalyzer = new QueryAutoStopWordAnalyzer(Version.LUCENE_29, analyzer); // should be done at class loading time
        //timed(printTime("Adding auto stopwords took ")) {
        //  queryTimeAnalyzer.addStopWords(contextSearcher.getIndexReader, DBpediaResourceField.CONTEXT.toString, 0.5f);
        //}
        // return this.queryTimeAnalyzer();

        return this.defaultAnalyzer();
    }

    public Query getQuery(Text context) throws SearchException {

        QueryParser parser = new QueryParser(Version.LUCENE_29, DBpediaResourceField.CONTEXT.toString(), getQueryTimeContextAnalyzer());
        Query ctxQuery = null;

        //escape special characters in Text before querying
        // + - && || ! ( ) { } [ ] ^ " ~ * ? : \
        //http://lucene.apache.org/java/3_0_2/queryparsersyntax.html#Escaping
        String queryText = context.text().replaceAll("[\\+\\-\\|!\\(\\)\\{\\}\\[\\]\\^~\\*\\?\"\\\\:&]", " ");
        queryText = QueryParser.escape(queryText);
        try {
            ctxQuery = parser.parse(queryText);
        } catch (ParseException e) {
            StringBuffer msg = new StringBuffer();
            msg.append("Error parsing context. ");
            if (e.getMessage().contains("too many boolean clauses")) {
                msg.append(String.format("QueryParser broke with %s tokens.",queryText.split("\\W+").length));
            }
            msg.append("\n");
            msg.append(context);
            msg.append("\n");
            e.printStackTrace();
            throw new SearchException(msg.toString(),e);
        }
        return ctxQuery;
    }

    public Query getQuery(SurfaceForm sf) throws SearchException {
        Term sfTerm = new Term(DBpediaResourceField.SURFACE_FORM.toString(),sf.name());
        return new TermQuery(sfTerm); //TODO FIXME PABLO CandidateResourceQuery
    }

    public Query getQuery(DBpediaResource resource) {
        return new TermQuery(new Term(DBpediaResourceField.URI.toString(),
                resource.uri()));
    }

    /**
     * Query to retrieve all DBpedia Resources in the index that are within a set of allowed URIs and match the input text.\
     * Uses Lucene's MoreLikeThis rather than ICF as defined in DBpedia Spotlight's paper.
     * It is faster, but doesn't take into account selectional preferences of words wrt resources.
     *
     * @param text
     * @param allowedUris
     * @return
     */
    public Query getQuery(Text text, Set<DBpediaResource> allowedUris, IndexReader reader) throws IOException {
        //val filter = new FieldCacheTermsFilter(DBpediaResourceField.CONTEXT.toString,allowedUris)
        TermsFilter filter = new org.apache.lucene.search.TermsFilter();
        for (DBpediaResource u:  allowedUris) {
            filter.addTerm(new Term(DBpediaResourceField.URI.toString(),u.uri()) );
        }
        MoreLikeThis mlt = new MoreLikeThis(reader);
        String[] fields = {DBpediaResourceField.CONTEXT.toString()};
        mlt.setFieldNames(fields);
        mlt.setAnalyzer(this.mDefaultAnalyzer);
        InputStream inputStream = new ByteArrayInputStream(text.text().getBytes("UTF-8"));
        Query query = mlt.like(inputStream);
        return query;
    }



    /* ---------------------- Basic methods for indexing correction ------------------------------- */
    //TODO move to LuceneFieldFactory
    public Field getField(Text text) {
        return new Field(LuceneManager.DBpediaResourceField.CONTEXT.toString(),
                text.text(),
                Field.Store.YES,   // has to be stored if the index is enriched later, otherwise context is lost when loading a Document in memory
                Field.Index.ANALYZED,
                Field.TermVector.YES); //(PABLO 27/Jul) it used to store positions. removed it to save time/space
    }

    public Field getField(DBpediaResource resource) {
        return new Field(LuceneManager.DBpediaResourceField.URI.toString(),
                        resource.uri(),
                        Field.Store.YES,
                        Field.Index.NOT_ANALYZED_NO_NORMS);
    }

    public Field getField(SurfaceForm surfaceForm) {
        return new Field(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString(),
                        surfaceForm.name(),
                        Field.Store.YES,
                        Field.Index.NOT_ANALYZED);//03/Dec Added Norms for using normalized TF as prior (conditional). Was: Field.Index.NOT_ANALYZED_NO_NORMS); 
    }

    public Field getField(DBpediaType t) {
        return new Field(DBpediaResourceField.TYPE.toString(),
                                    t.name(),
                                    Field.Store.YES,
                                    Field.Index.NOT_ANALYZED_NO_NORMS);
    }

    public Field getUriCountField(int startCount) {
        return new Field(DBpediaResourceField.URI_COUNT.toString(),
                                    Integer.toString(startCount),
                                    Field.Store.YES,
                                    Field.Index.NOT_ANALYZED_NO_NORMS);
    }

    public Field getUriPriorField(double prior) {
        return new Field(DBpediaResourceField.URI_PRIOR.toString(),
                                    Double.toString(prior),
                                    Field.Store.YES,
                                    Field.Index.NOT_ANALYZED_NO_NORMS);
    }

    /*---------- Composed methods for querying the index correctly (they use the basic methods) ----------*/ //TODO Move to LuceneQueryFactory

//    public Query getQuery(SurfaceForm sf, Text context) throws SearchException {  //PABLO add specific query for sf+context case
//        BooleanQuery query = new BooleanQuery(); //TODO look closer at the behavior of BooleanQuery
//        BooleanClause sfClause = new BooleanClause(getQuery(sf), BooleanClause.Occur.MUST);
//        BooleanClause ctxClause = new BooleanClause(getQuery(context), BooleanClause.Occur.SHOULD);
//        query.add(sfClause);
//        query.add(ctxClause);
//        return query;
//    }

    /**
     * Builds a Lucene Query object (CandidateResourceQuery) that uses the SurfaceForm to compute ICF for context terms
     * @param sf
     * @param context
     * @return
     * @throws SearchException
     */
    public BooleanQuery getQuery(SurfaceForm sf, Text context) throws SearchException {
        // First get all terms in the context
        Set<Term> ctxTerms = new HashSet<Term>();
        Query contextQuery = getQuery(context);
        contextQuery.extractTerms(ctxTerms);

        // Then get the surface form terms
        Query sfQuery = getQuery(sf);
        Set<Term> sfTerms = new HashSet<Term>();
        sfQuery.extractTerms(sfTerms);   //TODO FUTURE if we have an ngram analyzer for SF, passing this set down to the similarity class enables its usage. For now we have only one term.

        // Now create CandidateResourceQueries that associate the surface form to each context term
        BooleanQuery orQuery = new BooleanQuery();
        orQuery.add(new BooleanClause(sfQuery, BooleanClause.Occur.MUST)); //TODO do we need this?
        for (Term sfTerm: sfTerms) { //FIXME this is not correct in the context of the ICF similarity. better to pass the full set downstream and let they handle it there. but for now, we have only one term..
            for (Term t: ctxTerms) {
                orQuery.add(new CandidateResourceQuery(sfTerm, t), BooleanClause.Occur.SHOULD);
            }
        }
        return orQuery;
    }

    public Query getQuery(DBpediaResource resource, Text context) throws SearchException {
        BooleanQuery query = new BooleanQuery(); //TODO look closer at the behavior of BooleanQuery
        BooleanClause resClause = new BooleanClause(getQuery(resource), BooleanClause.Occur.MUST);
        BooleanClause ctxClause = new BooleanClause(getQuery(context), BooleanClause.Occur.SHOULD);
        query.add(resClause);
        query.add(ctxClause);
        return query;
    }

    /*---------------------- Composed methods for indexing correctly -------------------------------*/
    
    /**
     * Creates a new document from the resourceOccurrence (to be stored in index)
     * @param resourceOccurrence
     * @return
     */
    public Document createDocument(DBpediaResourceOccurrence resourceOccurrence) {
        Document doc = new Document();
        doc.add(getField(resourceOccurrence.resource()));
        doc.add(getField(resourceOccurrence.surfaceForm()));  //uncomment this if you want anchor texts as surface forms; otherwise index surface forms in a second run together with types
        doc.add(getField(resourceOccurrence.context()));
        doc.add(getUriCountField(resourceOccurrence.resource().support()));
        doc.add(getUriPriorField(resourceOccurrence.resource().prior()));
        return doc;
    }

    /**
     * Creates a new document from a wikiPageContext (to be stored in index)
     * @param wikiPageContext
     * @return
     */
    public Document createDocument(WikiPageContext wikiPageContext) {
        Document doc = new Document();
        doc.add(getField(wikiPageContext.context()));
        doc.add(getField(wikiPageContext.resource()));
        return doc;
    }

    /**
     * Creates a new document from a paragraph
     * @param text
     * @return
     */
    public Document createDocument(Text text) {
        Document doc = new Document();
        doc.add(getField(text));
        return doc;
    }


    /**
     * Creates a new document from surface form and resource (to be stored in index)
     * TODO why do we need this in addition to DBpediaResourceOccurrence which already encapsulates both? because CandidateIndexer does not have Text. Want to abuse?
     * @param surfaceForm
     * @param resource
     * @return
     */
    public Document createDocument(SurfaceForm surfaceForm, DBpediaResource resource) {
        Document doc = new Document();
        doc.add(getField(surfaceForm));
        doc.add(getField(resource));
        doc.add(getUriCountField(resource.support()));
        doc.add(getUriPriorField(resource.prior()));
        return doc;
    }
    public Document createDocument(SurfaceForm surfaceForm, DBpediaResource resource, int nTimes) {
        Document doc = new Document();
        for (int i=0; i<nTimes; i++) doc.add(getField(surfaceForm));
        doc.add(getField(resource));
        doc.add(getUriCountField(nTimes));
        doc.add(getUriPriorField(resource.prior()));
        return doc;
    }

    public Document addOccurrenceToDocument(DBpediaResourceOccurrence occ, Document doc) {
        Document occDoc = createDocument(occ);
        return merge(occDoc, doc);
    }


    /**
     * LuceneManager subclass that overrides {@link #getField} and {@link #getQuery} to act case insensitive for surface forms.
     * @author pablomendes
     */
    public static class CaseInsensitiveSurfaceForms extends LuceneManager {

        public CaseInsensitiveSurfaceForms(Directory dir) throws IOException {
            super(dir);
        }

        // Make getField case insensitive
        @Override
        public Field getField(SurfaceForm sf) {
            return super.getField(new SurfaceForm(sf.name().toLowerCase()));
        }

        // Make getQuery case insensitive
        @Override
        public Query getQuery(SurfaceForm sf) throws SearchException {
            return super.getQuery(new SurfaceForm(sf.name().toLowerCase()));
        }
    }

    /**
     * LuceneManager subclass that uses an EdgeNGramAnalyzer to do approximate matching of surface forms
     * @author pablomendes
     */
    public static class CaseSensitiveSurfaceForms extends LuceneManager {

        // How to break down the input text
        private Analyzer mSurfaceFormAnalyzer = new NGramAnalyzer(3,3);
        Directory mCandidateIndexDir;

        public CaseSensitiveSurfaceForms(Directory dir) throws IOException {
            super(dir);
            PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_29));
            perFieldAnalyzer.addAnalyzer(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString(),mSurfaceFormAnalyzer);
            setDefaultAnalyzer(perFieldAnalyzer);
        }

        public CaseSensitiveSurfaceForms(Directory contextIndexDir, Directory sfIndexDir) throws IOException {
            super(contextIndexDir);
            PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_29));
            perFieldAnalyzer.addAnalyzer(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString(),mSurfaceFormAnalyzer);
            setDefaultAnalyzer(perFieldAnalyzer);
        }

        @Override
        public Field getField(SurfaceForm surfaceForm) {
            return new Field(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString(),
                            surfaceForm.name(),
                            Field.Store.YES,
                            Field.Index.ANALYZED);
        }

        @Override
        public Query getQuery(SurfaceForm sf) throws SearchException {

            QueryParser parser = new QueryParser(Version.LUCENE_29, DBpediaResourceField.SURFACE_FORM.toString(), mSurfaceFormAnalyzer);
            Query sfQuery = null;
            //TODO escape (instead of remove) special characters in Text before querying
            // + - && || ! ( ) { } [ ] ^ " ~ * ? : \
            //http://lucene.apache.org/java/3_0_2/queryparsersyntax.html#Escaping
            String queryText = sf.name().replaceAll("[\\+\\-\\|!\\(\\)\\{\\}\\[\\]\\^~\\*\\?\"\\\\:&]", " ");
            queryText = QueryParser.escape(queryText);
            try {
                sfQuery = parser.parse(queryText);
            } catch (ParseException e) {
                StringBuffer msg = new StringBuffer();
                msg.append("Error parsing surface form. ");
                if (e.getMessage().contains("too many boolean clauses")) {
                    msg.append(String.format("QueryParser broke with %s tokens.",queryText.split("\\W+").length));
                }
                msg.append("\n");
                msg.append(sf);
                msg.append("\n");
                e.printStackTrace();
                throw new SearchException(msg.toString(),e);
            }
            return sfQuery;
        }

    }




    /**
     * LuceneManager subclass used by the {@link org.dbpedia.spotlight.lucene.index.MergedOccurrencesContextIndexer}
     * It stores buffering configuration information.
     */
    public static class BufferedMerging extends LuceneManager {

        // will buffer these many documents in memory before merging them to disk
        protected int minNumDocsBeforeFlush = 100000;  // DEFAULT value
        // will write to disk this many times before optimizing; OPTIMIZING DOES NOT INCREASE SEARCH PERFORMANCE: NO INTERMEDIATE OPTIMIZES!!
        protected int maxMergesBeforeOptimize = Integer.MAX_VALUE;
        // flag to set final optimize: would decrease disk space but takes long
        protected boolean lastOptimize = true;

        public BufferedMerging(Directory directory) throws IOException {
            super(directory);
        }
        public BufferedMerging(Directory directory, boolean lastOptimize) throws IOException {
            super(directory);
            this.lastOptimize = lastOptimize;
        }
        public BufferedMerging(Directory directory, int minNumDocsBeforeFlush) throws IOException {
            super(directory);
            this.minNumDocsBeforeFlush = minNumDocsBeforeFlush;
        }
        public BufferedMerging(Directory directory, int minNumDocsBeforeFlush, boolean lastOptimize) throws IOException {
            super(directory);
            this.minNumDocsBeforeFlush = minNumDocsBeforeFlush;
            this.lastOptimize = lastOptimize;
        }
//        public BufferedMerging() throws IOException {
//            super(FSDirectory.open(new File("")));
//        }

        
        public int minNumDocsBeforeFlush() {
            return minNumDocsBeforeFlush;
        }

        public int maxMergesBeforeOptimize() {
            return maxMergesBeforeOptimize;
        }

        public boolean lastOptimize() {
            return lastOptimize;
        }

//                // Make getField case insensitive
//        @Override
//        public Field getField(SurfaceForm sf) {
//            return super.getField(new SurfaceForm(sf.name().toLowerCase()));
//        }
//
//        // Make getQuery case insensitive
//        @Override
//        public Query getQuery(SurfaceForm sf) {
//            return super.getQuery(new SurfaceForm(sf.name().toLowerCase()));
//        }
    }

    public static class SeparateSearchers extends LuceneManager {

        //Directory mContextIndexDir is in superclass
        Directory mSurrogateIndexDir;

        public SeparateSearchers(Directory surrogateIndexDir, Directory contextIndexDir) throws IOException {
            super(contextIndexDir);
            mSurrogateIndexDir = surrogateIndexDir;
        }

        public Directory contextIndexDir() {
            return mContextIndexDir;
        }

        public Directory surrogateIndexDirectory() {
            return mSurrogateIndexDir;
        }

    }

}
