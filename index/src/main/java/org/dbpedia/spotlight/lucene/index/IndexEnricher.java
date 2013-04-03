/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.lucene.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Similarity;
import org.dbpedia.spotlight.exceptions.IndexException;

import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.util.IndexingConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Class adding surface forms and DBpedia types to an existing index that contains URIs and context (both "stored").
 *
 * @author maxjakob
 * @author pablomendes (prior and count enrichers) TODO consider splitting each of the enrichWith... methods into a subclass of IndexEnricher
 */
public class IndexEnricher extends BaseIndexer<Object> {

    Log LOG = LogFactory.getLog(this.getClass());

    int DOCS_BEFORE_FLUSH = 10000;  // for priored surface forms (failed with 20,000 before (without PRIOR_DEVIDER))

    MergedOccurrencesContextSearcher searcher;

    Analyzer mAnalyzer;

    /**
     * See {@link BaseIndexer}
     * @param sourceIndexManager
     * @throws java.io.IOException
     */
    private IndexEnricher(LuceneManager sourceIndexManager, LuceneManager targetIndexManager, IndexingConfiguration config) throws IOException {
        super(targetIndexManager, true); //ATTENTION: if this is set to true, it will override the existing index!
        searcher = new MergedOccurrencesContextSearcher(sourceIndexManager);
        mAnalyzer = config.getAnalyzer();
        LOG.info("Analyzer class: "+mAnalyzer.getClass());
    }

    public IndexEnricher(String sourceIndexFileName, String targetIndexFileName, IndexingConfiguration config) throws IOException{
        this(getSourceManager(sourceIndexFileName, config), getTargetManager(targetIndexFileName, config), config);
    }

    public static LuceneManager getSourceManager(String fileName, IndexingConfiguration config) throws IOException {
        File indexFile = new File(fileName);
        if (!indexFile.exists())
            throw new IOException("source index dir "+indexFile+" does not exist; ");
        LuceneManager lucene = new LuceneManager.BufferedMerging(LuceneManager.pickDirectory(indexFile));
        lucene.setDefaultAnalyzer(config.getAnalyzer());
        return lucene;
    }
    public static LuceneManager getTargetManager(String fileName, IndexingConfiguration config) throws IOException {
        File indexFile = new File(fileName);
        if (indexFile.exists())
            throw new IOException("target index dir "+indexFile+" exists; I am afraid of overwriting. ");
        LuceneManager lucene = new LuceneManager.BufferedMerging(LuceneManager.pickDirectory(indexFile));
        lucene.setDefaultAnalyzer(config.getAnalyzer());
        return lucene;
    }


    public void expunge() throws IOException {
        mWriter.expungeDeletes();
        mWriter.commit();
    }

    private long getIndexSize() {
        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only enrich existing indexes");
        }
        return indexSize;
    }

    private void commit(int i) throws IOException {
        if (i>0 && i%DOCS_BEFORE_FLUSH==0) {
            LOG.info("  processed "+i+" documents. committing...");
            mWriter.commit();
            LOG.info("  done.");
        }
        if (i%1000==0) {
            LOG.info(String.format("  processed %d documents. ",i));
        }
    }

    private void done(long indexSize) throws IndexException {
        LOG.info("Processed " + indexSize + " documents. Final commit...");
        try {
            mWriter.commit();
            LOG.info("Expunge deletes...");
            mWriter.expungeDeletes();
        } catch (IOException e) {
            throw new IndexException("Error while performing final commit for index enrichment.", e);
        }
        //LOG.info("Optimizing...");
        //mWriter.optimize();
        LOG.info("Done.");
    }

    public void enrichWithSurfaceForms(Map<String,LinkedHashSet<SurfaceForm>> sfMap) throws SearchException, IOException, IndexException {
        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add surface forms to an existing index");
        }
        LOG.info("Adding surface forms to index "+mLucene.directory()+"...");

        if (sfMap == null) {
            sfMap = new HashMap<String,LinkedHashSet<SurfaceForm>>();
        }

        for (int i=0; i<indexSize; i++) {
            if (!searcher.isDeleted(i)) {
                Document doc = searcher.getFullDocument(i);
                String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();

                LinkedHashSet<SurfaceForm> extraSfs = sfMap.remove(uri);
                if (extraSfs != null) {
                    for (SurfaceForm sf : extraSfs) {
                        int numberOfAdds = 1;
                        for (int j=0; j<numberOfAdds; j++) {
                            doc = mLucene.add(doc, sf);
                        }
                    }
                }

                Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
                mWriter.updateDocument(uriTerm, doc);  //deletes everything with this uri and writes a new doc

                commit(i);
            }
        }

        done(indexSize);
    }

    public void enrichWithCounts(Map<String,Integer> uriCountMap) throws SearchException, IOException, IndexException {
        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add URI counts to an existing index");
        }
        LOG.info("Adding URI counts to index "+mLucene.directory()+"...");

        if (uriCountMap == null) {
            uriCountMap = new HashMap<String,Integer>();
        }

        for (int i=0; i<indexSize; i++) {
            if (!searcher.isDeleted(i)) {
                Document doc = searcher.getFullDocument(i);
                String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();

                int count = uriCountMap.get(uri);
                doc = mLucene.add(doc, count);

                Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
                mWriter.updateDocument(uriTerm, doc);  //deletes everything with this uri and writes a new doc

                commit(i);
            }
        }

        done(indexSize);
    }

    public void enrichWithTypes(Map<String,LinkedHashSet<OntologyType>> typesMap) throws SearchException, IOException, IndexException {
        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add types to an existing index");
        }
        LOG.info("Adding types to  index "+mLucene.directory()+"...");

        if (typesMap == null) {
            LOG.error("Types map was empty. Done.");
            return;
        }

        for (int i=0; i<indexSize; i++) {
                Document doc = searcher.getFullDocument(i);
                String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();

                LinkedHashSet<OntologyType> types = typesMap.get(uri);
                if (types != null) {
                    for (OntologyType t : types) {
                        int numberOfAdds = 1;
                        for (int j=0; j<numberOfAdds; j++) {
                            doc = mLucene.add(doc, t);
                        }
                    }
                }
                Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
                mWriter.updateDocument(uriTerm, doc);  //deletes everything with this uri and writes a new doc

                commit(i);
        }

        done(indexSize);
    }


    public void patchAll(Map<String,LinkedHashSet<OntologyType>> typesMap, Map<String,Integer> uriCountMap, Map<String,LinkedHashSet<SurfaceForm>> sfMap) throws SearchException, IOException, IndexException {
        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only patch an existing index");
        }
        LOG.info("Patching index "+mLucene.directory()+"...");

        if (typesMap == null || uriCountMap == null || sfMap == null) {
            throw new IllegalArgumentException("types, uri counts and surface forms should be populated.");
        }

        for(int i=0; i<indexSize; i++) {
            if (!searcher.isDeleted(i)) {
                Document doc = searcher.getFullDocument(i);
                String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();

                // add types
                LinkedHashSet<OntologyType> types = typesMap.get(uri);
                if (types != null) for (OntologyType t : types) doc = mLucene.add(doc, t);
                // add counts
                doc = mLucene.add(doc, uriCountMap.get(uri));
                // add surface forms
                LinkedHashSet<SurfaceForm> extraSfs = sfMap.remove(uri);
                if (extraSfs != null) for (SurfaceForm sf : extraSfs) doc = mLucene.add(doc, sf);

                // update document in index
                Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
                mWriter.updateDocument(uriTerm, doc);  //deletes everything with this uri and writes a new doc

                // write to disk for every 10000 or so entries
                commit(i);
            }
        }

        done(indexSize);
    }
    /**
     * Goes through the index and unstores surface forms and context.
     *
     * @throws SearchException: inherited from searcher.getFullDocument
     * @throws IOException: inherited from mWriter.updateDocument
     */
    public void unstore(List<LuceneManager.DBpediaResourceField> unstoreFields, int optimizeSegments) throws SearchException, IOException {
        unstore(unstoreFields,optimizeSegments,0);
    }
    public void unstore(List<LuceneManager.DBpediaResourceField> unstoreFields, int optimizeSegments, int minCount) throws SearchException, IOException {
        //List<LuceneManager.DBpediaResourceField> unstoreFields = new LinkedList<LuceneManager.DBpediaResourceField>();

        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only unstore fields of an existing index");
        }
        LOG.info("Unstoring "+unstoreFields+" in index "+mLucene.directory()+"...");
        for (int i=0; i<indexSize; i++) {
            if (!searcher.isDeleted(i)) {
                LOG.trace("URI_COUNT did not exist. Creating from multiple URI fields.");
                Document doc = searcher.getFullDocument(i);
                String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();

                int support = 0;
                Field uriCount = doc.getField(LuceneManager.DBpediaResourceField.URI_COUNT.toString());
                if (uriCount==null) {
                    Field[] uriFields = doc.getFields(LuceneManager.DBpediaResourceField.URI.toString());
                    support = uriFields.length;
                    uriCount = this.mLucene.getUriCountField(support);
                    doc.add(uriCount); // add count
                    doc.removeFields(LuceneManager.DBpediaResourceField.URI.toString()); // remove repeated fields
                    doc.add(uriFields[0]); // add only once
                }
                else support = new Integer(uriCount.stringValue());
                LOG.trace(String.format("URI count for %s = %d",uri,support));

                Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
                if (support<minCount) {
                    LOG.debug(String.format("Dropping %s; count = %d",uri,support));
                    mWriter.deleteDocuments(uriTerm);
                } else {
                    doc = mLucene.unstore(doc, unstoreFields);
                    mWriter.updateDocument(uriTerm, doc); //deletes everything with this uri and writes a new doc
                }
                commit(i);
            }
        }

        LOG.info("Processed "+indexSize+" documents. Final commit...");
        mWriter.commit();

        if(optimizeSegments > 0) {
            LOG.info("Optimizing...");
            mWriter.optimize(optimizeSegments);
            mWriter.commit();
        } else {
            LOG.info("Expunge deletes...");
            mWriter.expungeDeletes();
        }

        LOG.info("Done.");
    }

    public void add(Object o) {
        //TODO re-factoring to make this an
    }

}
