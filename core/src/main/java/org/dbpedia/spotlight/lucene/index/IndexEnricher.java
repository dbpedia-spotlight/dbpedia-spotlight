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


    public void enrichWithSurfaceForms(Map<String,LinkedHashSet<SurfaceForm>> sfMap) throws SearchException, IOException {
        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add surface forms to an existing index");
        }
        LOG.info("Adding surface forms to index "+mLucene.directory()+"...");

        if (sfMap == null) {
            sfMap = new HashMap<String,LinkedHashSet<SurfaceForm>>();
        }

        for (int i=0; i<indexSize; i++) {
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

    public void enrichWithTypes(Map<String,LinkedHashSet<DBpediaType>> typesMap) throws SearchException, IOException {
        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only add types to an existing index");
        }
        LOG.info("Adding types to  index "+mLucene.directory()+"...");

        if (typesMap == null) {
            typesMap = new HashMap<String,LinkedHashSet<DBpediaType>>();
        }

        for (int i=0; i<indexSize; i++) {
            Document doc = searcher.getFullDocument(i);
            String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();

            LinkedHashSet<DBpediaType> types = typesMap.get(uri);
            if (types != null) {
                for (DBpediaType t : types) {
                    int numberOfAdds = 1;
                    for (int j=0; j<numberOfAdds; j++) {
                        doc = mLucene.add(doc, t);
                    }
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

    /**
     * Goes through the index and unstores surface forms and context.
     *
     * @throws SearchException: inherited from searcher.getFullDocument
     * @throws IOException: inherited from mWriter.updateDocument
     */
    public void unstore(List<LuceneManager.DBpediaResourceField> unstoreFields, int optimizeSegments) throws SearchException, IOException {
        //List<LuceneManager.DBpediaResourceField> unstoreFields = new LinkedList<LuceneManager.DBpediaResourceField>();

        long indexSize = searcher.getNumberOfEntries();
        if (indexSize == 0) {
            throw new IllegalArgumentException("index in "+mLucene.directory()+" contains no entries; this method can only unstore fields of an existing index");
        }
        LOG.info("Unstoring "+unstoreFields+" in index "+mLucene.directory()+"...");
        for (int i=0; i<indexSize; i++) {
            Document doc = searcher.getFullDocument(i);
            String uri = doc.getField(LuceneManager.DBpediaResourceField.URI.toString()).stringValue();
            doc = mLucene.unstore(doc, unstoreFields);
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
        if(optimizeSegments > 0) {
            LOG.info("Optimizing...");
            mWriter.optimize(optimizeSegments);
            mWriter.commit();
        }
        LOG.info("Done.");
    }

    public void unstore(List<LuceneManager.DBpediaResourceField> unstoreFields) throws SearchException, IOException {
        unstore(unstoreFields, 0);
    }

    public void add(Object o) {
        //TODO re-factoring to make this an
    }

}
