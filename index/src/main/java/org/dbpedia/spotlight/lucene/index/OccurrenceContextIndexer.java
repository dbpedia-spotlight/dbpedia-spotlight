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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.dbpedia.spotlight.exceptions.IndexException;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.IOException;

/**
 * Abstract class for indexers that take DBpediaResourceOccurrences for indexing.
 * See {@link MergedOccurrencesContextIndexer} and {@link SeparateOccurrencesIndexer} for concrete implementations.
 */
public abstract class OccurrenceContextIndexer extends BaseIndexer<DBpediaResourceOccurrence> {

    Log LOG = LogFactory.getLog(this.getClass());
    
    /**
     * See {@link org.dbpedia.spotlight.lucene.index.BaseIndexer}
     * @param lucene
     * @param create
     * @throws IOException
     */
    public OccurrenceContextIndexer(LuceneManager lucene, boolean create) throws IOException {
        super(lucene, create);
    }

    /**
     * See {@link org.dbpedia.spotlight.lucene.index.BaseIndexer}
     * @param lucene
     * @throws IOException
     */
    public OccurrenceContextIndexer(LuceneManager lucene) throws IOException {
        super(lucene);
    }

    public abstract void add(DBpediaResourceOccurrence resourceOccurrence) throws IndexException;

    /**
     * This method just adds an occurrence to the index without trying to merge.
     * It is used by the indexer for definition pages ({@link SeparateOccurrencesIndexer}).
     * If you want to merge all occurrences of a given DBpediaResource into the same vector, see {@link MergedOccurrencesContextIndexer}).
     * @param resourceOccurrence
     * @throws IndexException
     */
    protected void addOccurrence(DBpediaResourceOccurrence resourceOccurrence) throws IndexException {
        //LOG.debug("Indexing occurrence");
        Document newDoc = mLucene.createDocument(resourceOccurrence);
        try {
            mWriter.addDocument(newDoc); // do not commit for faster indexing.
        } catch (IOException e) {
            throw new IndexException("Error adding occurrence to the index. ",e);
        }
    }

    /**
     * This method adds an occurrence to an existing resource in the index -- i.e. it merges the new occurrence into the corresponding entry in the index.
     * @param resourceOccurrence
     * @throws IndexException -
     */
    protected void updateOccurrence(DBpediaResourceOccurrence resourceOccurrence) throws IndexException {
        try {
            IndexSearcher searcher = new IndexSearcher(this.mLucene.mContextIndexDir, true);// read-only=true

            Term idTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(),
                    resourceOccurrence.resource().uri());
            Query query = new TermQuery(idTerm);
            ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
            LOG.debug(hits.length+" hits found.");
            if (hits.length>0) { //Update existing resource
                for (ScoreDoc hit: hits) {
                    Document doc = searcher.doc(hit.doc);
                    doc = mLucene.addOccurrenceToDocument(resourceOccurrence, doc);
                    mWriter.updateDocument(idTerm, doc);
                    //TermFreqVector tfv = searcher.getIndexReader().getTermFreqVector(docNo, LuceneManager.Field.CONTEXT);
                }
            } else {
                addOccurrence(resourceOccurrence);
            }
            mWriter.commit(); //Commit for every occurrence is not efficient, but we do because we merge every new occurrence into a resource.
            searcher.close();
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }


}
