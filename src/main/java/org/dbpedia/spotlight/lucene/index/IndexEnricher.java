package org.dbpedia.spotlight.lucene.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher;
import org.dbpedia.spotlight.model.DBpediaType;
import org.dbpedia.spotlight.model.SurfaceForm;

import java.io.IOException;
import java.util.*;

/**
 * Class adding surface forms and DBpedia types to an existing index that contains URIs and context (both "stored").
 */
public class IndexEnricher extends BaseIndexer<Object> {

    Log LOG = LogFactory.getLog(this.getClass());

    int DOCS_BEFORE_FLUSH = 25000;  // for priored surface forms (failed with 20,000 before (without PRIOR_DEVIDER))
    int PRIOR_DEVIDER = 10;         // for priored surface forms, add SF only  number of times URI is indexed / PRIOR_DEVIDER 

    MergedOccurrencesContextSearcher searcher;

    /**
     * See {@link BaseIndexer}
     * @param lucene
     * @throws java.io.IOException
     */
    public IndexEnricher(LuceneManager lucene) throws IOException {
        super(lucene);
        searcher = new MergedOccurrencesContextSearcher(this.mLucene);
    }

    /**
     * Enrich index with surface forms and/or types.
     *
     * @param sfMap: Map from URI to set of surface forms.
     * @param typeMap: Map from URI to list of DBpedia types.
     * @param priored: If true, adds surface forms as many times as the URI was indexed.
     * @throws SearchException: inherited from searcher.getFullDocument
     * @throws IOException: inherited from mWriter.updateDocument
     */
    public void enrich(Map<String,LinkedHashSet<SurfaceForm>> sfMap, Map<String,LinkedHashSet<DBpediaType>> typeMap, Boolean priored) throws SearchException, IOException {
        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add surface forms to an existing index");
        }
        LOG.info("Enriching index "+mLucene.directory()+"...");

        if (sfMap == null) {
            sfMap = new HashMap<String,LinkedHashSet<SurfaceForm>>();
        }
        if (typeMap == null) {
            typeMap = new HashMap<String,LinkedHashSet<DBpediaType>>();
        }

        for (int i=0; i<indexSize; i++) {
            Document doc = searcher.getFullDocument(i);
            String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();

            LinkedHashSet<SurfaceForm> extraSfs = sfMap.get(uri);
            if (extraSfs != null) {
                for (SurfaceForm sf : extraSfs) {
                    int numberOfAdds = 1;
                    if (priored) {
                        numberOfAdds += searcher.getSupport(doc) / PRIOR_DEVIDER;  //TODO this is a hack for performance (big document don't fit in memory otherwise) and for smoothing
                    }
                    for (int j=0; j<numberOfAdds; j++) {
                        doc = mLucene.add(doc, sf);
                    }
                }
            }

            LinkedHashSet<DBpediaType> extraTypes = typeMap.get(uri);
            if (extraTypes != null) {
                for (DBpediaType t : extraTypes) {
                    doc = mLucene.add(doc, t);
                }
            }

            Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
            mWriter.updateDocument(uriTerm, doc);  //deletes everything with this uri and writes a new doc

            if (i%DOCS_BEFORE_FLUSH==0) {
                LOG.info("  processed "+i+" documents. committing...");
                mWriter.commit();
                LOG.info("  done.");
            }
        }

        LOG.info("Processed "+indexSize+" documents. Final commit...");
        mWriter.commit();
        //LOG.info("Optimizing...");
        //mWriter.optimize();
        LOG.info("Done.");
    }

    public void enrich(Map<String,LinkedHashSet<SurfaceForm>> map1, Map<String,LinkedHashSet<DBpediaType>> map2) throws SearchException, IOException {
        enrich(map1, map2, false);
    }

    /**
     * Goes through the index and unstores surface forms and context.
     *
     * @throws SearchException: inherited from searcher.getFullDocument
     * @throws IOException: inherited from mWriter.updateDocument
     */
    public void unstore() throws SearchException, IOException {
        List<LuceneManager.DBpediaResourceField> unstoreFields = new LinkedList<LuceneManager.DBpediaResourceField>();
        unstoreFields.add(LuceneManager.DBpediaResourceField.SURFACE_FORM);
        unstoreFields.add(LuceneManager.DBpediaResourceField.CONTEXT);

        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only unstore fields of an existing index");
        }
        LOG.info("Unstoring "+unstoreFields+" in index "+mLucene.directory()+"...");
        for (int i=0; i<indexSize; i++) {
            Document doc = searcher.getFullDocument(i);
            String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();
            doc = mLucene.unstore(doc, unstoreFields);    //TODO see if this works and how fast
            Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
            mWriter.updateDocument(uriTerm, doc); //deletes everything with this uri and writes a new doc

            if (i%DOCS_BEFORE_FLUSH==0) {
                LOG.info("  processed "+i+" documents. committing...");
                mWriter.commit();
                LOG.info("  done.");
            }
        }

        LOG.info("Processed "+indexSize+" documents. Final commit...");
        mWriter.commit();
        LOG.info("Optimizing...");
        mWriter.optimize();
        LOG.info("Done.");
    }

    public void add(Object o) {
        //TODO implement something sensible here. only created because BaseIndexer requires it.
    }

    
//    public void addTypesForAll(File typesMapTSVFile) throws IOException, SearchException {
//        long indexSize = searcher.getNumberOfEntries();
//        if (indexSize == 0) {
//            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add types to an existing index");
//        }
//        LOG.info("Adding types to all documents in the index"+mLucene.directory()+"...");
//        Map<String,List<DBpediaType>> typesMap = TypesLoader.getTypesMap_java(typesMapTSVFile);
//        for (int i=0; i<indexSize; i++) {
//            Document doc = searcher.getFullDocument(i);
//            String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();
//            List<DBpediaType> typesList = typesMap.get(uri);
//            if (typesList != null) {
//                doc = mLucene.addTypes(doc, typesList);
//            }
//            Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
//            mWriter.updateDocument(uriTerm, doc); //deletes everything with this uri and writes a new doc
//            if (i%100000==0) {
//                LOG.info("  processed "+i+" documents. committing...");
//                mWriter.commit();
//                LOG.info("  done.");
//            }
//        }
//        LOG.info("Processed "+indexSize+" documents. Final commit...");
//        mWriter.commit();
//        LOG.info("Optimizing...");
//        mWriter.optimize();
//        LOG.info("Done.");
//    }
//
//    /**
//     * Call this for merged indeces. (probably only with the surrogate set consisting of titles, redirects
//     * and disambiguations because the occurrence surface forms are already in there)
//     *
//     * @param surrogatesTSVFile
//     * @throws IOException
//     */
//    public void addSurfaceFormsForAll(File surrogatesTSVFile, Boolean lowerCased) throws IOException, SearchException {
//        long indexSize = searcher.getNumberOfEntries();
//        if (indexSize == 0) {
//            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add surface forms to an existing index");
//        }
//        LOG.info("Adding surface forms to all documents in the index"+mLucene.directory()+"...");
//        Map<String,List<SurfaceForm>> reverseSurrogatesMap = SurrogatesUtil.getReverseSurrogatesMap_java(surrogatesTSVFile, lowerCased);
//        for (int i=0; i<indexSize; i++) {
//            Document doc = searcher.getFullDocument(i);
//            String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();
//            List<SurfaceForm> surfaceForms = reverseSurrogatesMap.get(uri);
//            if (surfaceForms != null) {
//                doc = mLucene.addSurfaceForms(doc, surfaceForms);
//            }
//            Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
//            mWriter.updateDocument(uriTerm, doc); //deletes everything with this uri and writes a new doc
//            if (i%100000==0) {
//                LOG.info("  processed "+i+" documents. committing...");
//                mWriter.commit();
//                LOG.info("  done.");
//            }
//        }
//        LOG.info("Processed "+indexSize+" documents. Final commit...");
//        mWriter.commit();
//        LOG.info("Optimizing...");
//        mWriter.optimize();
//        LOG.info("Done.");
//    }
//
//    public void addSurfaceFormsAndTypes(File typesMapTSVFile, File surrogatesTSVFile, Boolean lowerCased) throws IOException, SearchException {
//        long indexSize = searcher.getNumberOfEntries();
//        if (indexSize == 0) {
//            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add surface forms to an existing index");
//        }
//        LOG.info("Adding surface forms to all documents in the index"+mLucene.directory()+"...");
//        Map<String,List<DBpediaType>> typesMap = TypesLoader.getTypesMap_java(typesMapTSVFile);
//        Map<String,List<SurfaceForm>> reverseSurrogatesMap = SurrogatesUtil.getReverseSurrogatesMap_java(surrogatesTSVFile, lowerCased);
//        for (int i=0; i<indexSize; i++) {
//            Document doc = searcher.getFullDocument(i);
//            String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();
//            List<SurfaceForm> surfaceForms = reverseSurrogatesMap.get(uri);
//            if (surfaceForms != null) {
//                doc = mLucene.addSurfaceForms(doc, surfaceForms);
//            }
//            List<DBpediaType> typesList = typesMap.get(uri);
//            if (typesList != null) {
//                doc = mLucene.addTypes(doc, typesList);
//            }
//            Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
//            mWriter.updateDocument(uriTerm, doc); //deletes everything with this uri and writes a new doc
//            if (i%100000==0) {
//                LOG.info("  processed "+i+" documents. committing...");
//                mWriter.commit();
//                LOG.info("  done.");
//            }
//        }
//        LOG.info("Processed "+indexSize+" documents. Final commit...");
//        mWriter.commit();
//        LOG.info("Optimizing...");
//        mWriter.optimize();
//        LOG.info("Done.");
//    }
//
//    public void addSurfaceFormsPrioredForAll(File surrogatesTSVFile, Boolean lowerCased) throws IOException, SearchException {
//        long indexSize = searcher.getNumberOfEntries();
//        if (indexSize == 0) {
//            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add surface forms to an existing index");
//        }
//        LOG.info("Adding surface forms to all documents in the index"+mLucene.directory()+"...");
//        Map<String,List<SurfaceForm>> reverseSurrogatesMap = SurrogatesUtil.getReverseSurrogatesMap_java(surrogatesTSVFile, lowerCased);
//        for (int i=0; i<indexSize; i++) {
//            Document doc = searcher.getFullDocument(i);
//            String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();
//            List<SurfaceForm> surfaceForms = reverseSurrogatesMap.get(uri);
//            if (surfaceForms != null) {
//                int support = searcher.getSupport(doc) - 1;   //TODO remove -1 if dealing with fresh indexes
//                for (int j=0; j<support; j++) {
//                    doc = mLucene.addSurfaceForms(doc, surfaceForms);
//                }
//            }
//            Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
//            mWriter.updateDocument(uriTerm, doc); //deletes everything with this uri and writes a new doc
//            if (i%100000==0) {
//                LOG.info("  processed "+i+" documents. committing...");
//                mWriter.commit();
//                LOG.info("  done.");
//            }
//        }
//        LOG.info("Processed "+indexSize+" documents. Final commit...");
//        mWriter.commit();
//        LOG.info("Optimizing...");
//        mWriter.optimize();
//        LOG.info("Done.");
//    }
//
//
//
//    public void addSurfaceFormsPrioredAndTypes(File surrogatesTSVFile, Boolean lowerCased) throws IOException, SearchException {
//        long indexSize = searcher.getNumberOfEntries();
//        if (indexSize == 0) {
//            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add surface forms to an existing index");
//        }
//        LOG.info("Adding surface forms to all documents in the index"+mLucene.directory()+"...");
//        Map<String,List<SurfaceForm>> reverseSurrogatesMap = SurrogatesUtil.getReverseSurrogatesMap_java(surrogatesTSVFile, lowerCased);
//        for (int i=0; i<indexSize; i++) {
//            Document doc = searcher.getFullDocument(i);
//            String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();
//            List<SurfaceForm> surfaceForms = reverseSurrogatesMap.get(uri);
//            for (SurfaceForm sf : surfaceForms) {
//                int support = searcher.getSupport(doc);
//                for (int j=0; j<support; j++) {
//                    doc = mLucene.add(doc, sf);
//                }
//            }
//
//            Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
//            mWriter.updateDocument(uriTerm, doc); //deletes everything with this uri and writes a new doc
//            if (i%100000==0) {
//                LOG.info("  processed "+i+" documents. committing...");
//                mWriter.commit();
//                LOG.info("  done.");
//            }
//        }
//        LOG.info("Processed "+indexSize+" documents. Final commit...");
//        mWriter.commit();
//        LOG.info("Optimizing...");
//        mWriter.optimize();
//        LOG.info("Done.");
//    }
    
}
