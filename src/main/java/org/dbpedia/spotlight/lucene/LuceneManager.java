package org.dbpedia.spotlight.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.util.MemUtil;

import java.io.File;
import java.io.IOException;
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

    /*TODO isnt it bad practice to have public fields?
    "Tips on Choosing an Access Level: (from http://download.oracle.com/docs/cd/E17409_01/javase/tutorial/java/javaOO/accesscontrol.html)
    If other programmers use your class, you want to ensure that errors from misuse cannot happen.
    Access levels can help you do this.
    Use the most restrictive access level that makes sense for a particular member.
    Use private unless you have a good reason not to."
    */

    // How to break down the input text
    private Analyzer mContextAnalyzer = new StandardAnalyzer(Version.LUCENE_29);

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
    public boolean shouldOverride = true;  //BE CAREFUL: indices will be overwritten if true !!!!!

    // this value specifies how many top results Lucene should return
    // 
    public int topResultsLimit = 3;  //TODO be careful with SurrogateSearchers!!!

    public LuceneManager(Directory directory) throws IOException {
        this.mContextIndexDir = directory;
    }

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

    public Analyzer contextAnalyzer() {
        return mContextAnalyzer;
    }

    public void setContextAnalyzer(Analyzer contextAnalyzer) {
        this.mContextAnalyzer = contextAnalyzer;
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
        return shouldOverride;
    }

    public int topResultsLimit() {
        return topResultsLimit;
    }


    public enum DBpediaResourceField {
        URI("URI"), SURFACE_FORM("SURFACE_FORM"), CONTEXT("CONTEXT"), TYPE("TYPE");
        private String name;
        DBpediaResourceField(String name) {
            this.name = name;
        }
        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * THIS IMPLEMENTATION DOENT WORK AS INITIALLY EXPECTED
     * The document can have no fields, because we test for not null.
     * @param doc1
     * @param doc2
     * @return
     */
    public Document merge1(Document doc1, Document doc2) {
        Document doc = new Document();
        for (DBpediaResourceField f: DBpediaResourceField.values()) {
            Field f1 = doc1.getField(f.toString());
            Field f2 = doc2.getField(f.toString());
            if (f1!=null)
                doc.add(f1);
            if (f2!=null)
                doc.add(f2);
        }
        return doc;
    }

    public Document merge(Document doc1, Document doc2) {
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
            if (enumField.equals(DBpediaResourceField.CONTEXT)) {
                index = Field.Index.ANALYZED;
            }

            String fieldString = enumField.toString();
            for (Field luceneField : doc.getFields(fieldString)) {
                Field newField = new Field(fieldString,
                                            luceneField.stringValue(),
                                            store,
                                            index);
                newDoc.add(newField);
            }
        }
        return newDoc;
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
        
        return this.contextAnalyzer();
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
        Set<Term> qTerms = new HashSet<Term>();
        orQuery.extractTerms(qTerms);        
        return getMustQuery(qTerms);
    }

    public Query getMustQuery(Set<Term> qTerms) {
        BooleanQuery andQuery = new BooleanQuery();
        for (Term t: qTerms) {
            andQuery.add(new TermQuery(t), BooleanClause.Occur.MUST);
        }
        return andQuery;
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
            if (e.getMessage().contains("too many boolean clauses")) {
                System.out.printf("Broke with %s tokens.", queryText.split("\\W+").length);
            }
            e.printStackTrace();
            throw new SearchException("Error parsing context in "+context,e);
        }
        return ctxQuery;
    }

    public Query getQuery(SurfaceForm sf) {
        return new TermQuery(new Term(DBpediaResourceField.SURFACE_FORM.toString(),
                sf.name()));
    }

    public Query getQuery(DBpediaResource resource) {
        return new TermQuery(new Term(DBpediaResourceField.URI.toString(),
                resource.uri()));
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


    /*---------- Composed methods for querying the index correctly (they use the basic methods) ----------*/
    //TODO Move to LuceneQueryFactory
    public Query getQuery(SurfaceForm sf, Text context) throws SearchException {
        BooleanQuery query = new BooleanQuery(); //TODO look closer at the behavior of BooleanQuery
        BooleanClause sfClause = new BooleanClause(getQuery(sf), BooleanClause.Occur.MUST);
        BooleanClause ctxClause = new BooleanClause(getQuery(context), BooleanClause.Occur.SHOULD);
        query.add(sfClause);
        query.add(ctxClause);
        return query;
    }

    public Query getQuery(DBpediaResource resource, Text context) throws SearchException {
        BooleanQuery query = new BooleanQuery(); //TODO look closer at the behavior of BooleanQuery
        BooleanClause resClause = new BooleanClause(getQuery(resource), BooleanClause.Occur.MUST);
        BooleanClause ctxClause = new BooleanClause(getQuery(context), BooleanClause.Occur.SHOULD);
        query.add(resClause);
        query.add(ctxClause);
        return query;
    }

    public Query getQuery(Set<DBpediaResource> resources, Text context) throws SearchException {
        //TODO is this the correct query for the following behavior?
        //(+URI:Political_philosophy CONTEXT:media) OR (+URI:Mass_Media CONTEXT:media) works with QueryParser (?)
        BooleanQuery query = new BooleanQuery(); //TODO don't know what happens here. Have to look closer
        for (DBpediaResource res : resources) {
            BooleanClause resClause = new BooleanClause(getQuery(res), BooleanClause.Occur.SHOULD);
            query.add(resClause);
        }
        BooleanClause ctxClause = new BooleanClause(getQuery(context), BooleanClause.Occur.SHOULD);
        query.add(ctxClause);
        return query;
    }

    /*---------------------- Composed methods for indexing correctly -------------------------------*/
    
    /**
     * @param resourceOccurrence
     * @return
     */
    public Document getDocument(DBpediaResourceOccurrence resourceOccurrence) {
        Document doc = new Document();
        doc.add(getField(resourceOccurrence.resource()));
        //doc.add(getField(resourceOccurrence.surfaceForm()));  //TODO uncomment, was just a test!!!!!!!!
        doc.add(getField(resourceOccurrence.context()));
        return doc;
    }

    public Document getDocument(WikiPageContext wikiPageContext) {
        Document doc = new Document();
        doc.add(getField(wikiPageContext.context()));
        doc.add(getField(wikiPageContext.resource()));
        return doc;
    }

    public Document getDocument(SurfaceForm surfaceForm, DBpediaResource resource) {
        Document doc = new Document();
        doc.add(getField(surfaceForm));
        doc.add(getField(resource));
        return doc;
    }

    public Document addOccurrenceToDocument(DBpediaResourceOccurrence occ, Document doc) {
        Document occDoc = getDocument(occ);
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
        public Query getQuery(SurfaceForm sf) {
            return super.getQuery(new SurfaceForm(sf.name().toLowerCase()));
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
