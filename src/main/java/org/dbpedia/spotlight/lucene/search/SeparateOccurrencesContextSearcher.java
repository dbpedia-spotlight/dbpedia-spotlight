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

    @Override
    public long getNumberOfEntries() {  //TODO why override with the same implementation?
        return super.getNumberOfEntries();
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
                results.add(getFullDocument(hit.doc));
            }
        } catch (Exception e) {
            throw new SearchException("Error searching for resource "+resource, e);
        }
        return results;
    }



}