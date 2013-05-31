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

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.ContextSearcher;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.model.vsm.FeatureVector;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SeparateOccurrencesContextSearcher extends BaseSearcher implements ContextSearcher {

    public SeparateOccurrencesContextSearcher(LuceneManager lucene) throws IOException {
        super(lucene);
    }

    public ScoreDoc[] getHits(DBpediaResource resource, Text context) throws SearchException {
        return getHits(mLucene.getQuery(resource,context));
    }

    public Document getDoc(int i) throws IOException {
        return mSearcher.doc(i);
    }

    /**
     * CONTEXT SEARCHER
     * Searches for DBpedia Resources based on context.
     * For instance, given a paragraph, returns the DBpediaResource that occurred
     * in the most similar context to that paragraph.
     *
     * @param text
     * @return
     * @throws org.dbpedia.spotlight.exceptions.SearchException
     */
    public List<FeatureVector> search(Text text) throws SearchException {
        //LOG.debug("Searching the index for text: "+text);
        List<FeatureVector> results = new LinkedList<FeatureVector>();
        try {
            results = search(mLucene.getQuery(text));
        } catch (Exception e) {
            throw new SearchException("Error searching for text "+text, e);
        }
        return results;
    }

    /**
     * CONTEXT SEARCHER
     * Searches for DBpedia Resources based on URI
     *
     * @param resource
     * @return
     * @throws SearchException
     */
    public List<FeatureVector> get(DBpediaResource resource) throws SearchException {
        //LOG.debug("Searching the index for resource: "+resource);
        List<FeatureVector> results = new LinkedList<FeatureVector>();
        try {
            results = search(mLucene.getQuery(resource));
        } catch (Exception e) {
            throw new SearchException("Error searching for resource "+resource, e);
        }
        return results;
    }

    public List<FeatureVector> get(DBpediaResource resource, Text context) throws SearchException {

        //LOG.debug("Searching the index for resource: "+resource);
        List<FeatureVector> results = new LinkedList<FeatureVector>();
        try {
            results = search(mLucene.getQuery(resource, context));
        } catch (Exception e) {
            throw new SearchException("Error searching for resource "+resource, e);
        }
        return results;
    }

    /**
     * TODO CONTEXT SEARCHER ---- used only in indexing. can be left in BufferedReseourceIndexer, maybe
     * @param resource
     * @return
     * @throws SearchException
     */
    public List<Document> getOccurrences(DBpediaResource resource) throws SearchException {
        //LOG.debug("Searching the index for resource: "+resource);
        List<Document> results = new ArrayList<Document>();
        try {
            for (ScoreDoc hit: getHits(mLucene.getQuery(resource))) {
                if (!isDeleted(hit.doc)) results.add(getFullDocument(hit.doc));
            }
        } catch (Exception e) {
            throw new SearchException("Error searching for resource "+resource, e);
        }
        return results;
    }



}