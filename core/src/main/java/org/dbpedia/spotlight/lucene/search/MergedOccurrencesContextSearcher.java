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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Similarity;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.LuceneFeatureVector;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.lucene.similarity.CachedSimilarity;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.model.CandidateSearcher;
import org.dbpedia.spotlight.model.vsm.FeatureVector;
import org.dbpedia.spotlight.string.ContextExtractor;

import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains a unified index of (surface form, uri, context)
 * - Allows one step disambiguation (faster than two steps: getSurrogate, then Disambiguate)
 * - Allows prior disambiguator to get a quick "default sense" based on the most popular URI for a given surface form
 * @author pablomendes
 */
public class MergedOccurrencesContextSearcher extends BaseSearcher implements ContextSearcher, CandidateSearcher {

    String[] onlyUriCount = {LuceneManager.DBpediaResourceField.URI_COUNT.toString()};
    String[] uriAndCount = {LuceneManager.DBpediaResourceField.URI.toString(),
                            LuceneManager.DBpediaResourceField.URI_COUNT.toString()};
    String[] onlyUri = {LuceneManager.DBpediaResourceField.URI.toString()};
    private String[] onlyUriAndTypes = {LuceneManager.DBpediaResourceField.URI.toString(),
                                        LuceneManager.DBpediaResourceField.URI_COUNT.toString(),
                                        LuceneManager.DBpediaResourceField.TYPE.toString()};

    long numDocs = super.getNumberOfEntries();

    public MergedOccurrencesContextSearcher(LuceneManager lucene) throws IOException {
        this(lucene,false);
    }

    public MergedOccurrencesContextSearcher(LuceneManager lucene, boolean inMemory) throws IOException {
        super(lucene, inMemory);
        mSearcher.setSimilarity(lucene.contextSimilarity());
    }

    public LuceneManager getLuceneManager() {
        return mLucene;
    }

    public Similarity getSimilarity() {
        return mLucene.contextSimilarity();
    }
    /**
      * MIXED ALL IN ONE DISAMBIGUATOR IMPLEMENTATION
      * Implementation to perform two steps at once.
      * Performs a SurrogateIndex#get(SurfaceForm) followed by a DBpediaResourceIndex#get(DBpediaResource)
      * @param
      * @return
      * @throws org.dbpedia.spotlight.exceptions.SearchException
      */
    public List<FeatureVector> getSurrogates(SurfaceForm sf) throws SearchException {
        //LOG.debug("Retrieving surrogates for surface form: "+sf);

        // search index for surface form
        List<FeatureVector> surrogates = new ArrayList<FeatureVector>();

        // Iterate through the results:
        for (ScoreDoc hit : getHits(mLucene.getQuery(sf))) {
            int docNo = hit.doc;

            DBpediaResource resource = getDBpediaResource(docNo);
            TermFreqVector vector = getVector(docNo);
            surrogates.add(new LuceneFeatureVector(resource, vector));
        }

        //LOG.debug(surrogates.size()+" surrogates found.");

        // return set of surrogates
        return surrogates;
    }

    /**
     * How many concepts co-occurred with this surface form in some paragraph, with the surface form annotated or unnanotated.
     * @param sf
     * @return
     * @throws SearchException
     */
    public long getConceptNeighborhoodCount(SurfaceForm sf) throws SearchException {
        long ctxFreq = 0;
        Similarity similarity = getSimilarity();
        if (similarity instanceof CachedSimilarity)
            ctxFreq =((CachedSimilarity) similarity).getTermCache().getPromiscuity(mReader, sf); // quick cached response
        else {
            ScoreDoc[] hits = getHits(mLucene.getMustQuery(new Text(sf.name())));    // without caching we need to search
            ctxFreq = hits.length;
        }
        LOG.trace("Context Frequency (Promiscuity) for "+sf+"="+ctxFreq);
        return ctxFreq;
    }

    /**
     * How many occurrences contain this surface form
     * @param sf
     * @return
     * @throws SearchException
     */
    public long getContextFrequency(SurfaceForm sf) throws SearchException {
        long ctxFreq = 0;
        Query q = mLucene.getQuery(new Text(sf.name()));
        Set<Term> qTerms = new HashSet<Term>();
        q.extractTerms(qTerms);
        ScoreDoc[] hits = getHits(mLucene.getMustQuery(qTerms));
        for (ScoreDoc d: hits) {
            TermFreqVector vector = null;
            try {
                vector = mReader.getTermFreqVector(d.doc, LuceneManager.DBpediaResourceField.CONTEXT.toString());
            } catch (IOException e) {
                throw new SearchException("Error getting term frequency vector. ", e);
            }
            if (vector!=null) {
                int freq = Integer.MAX_VALUE; // set to max value because will be used in Math.min below
                // get min
                for (Term t : qTerms) {
                    int[] freqs = vector.getTermFrequencies();
                    String[] terms = vector.getTerms();
                    int pos = Arrays.binarySearch(terms,t.text());
                    int termFreq = freqs[pos];
                    freq = Math.min(freq, termFreq); // estimate probability of phrase by the upper bound which is the minimum freq of individual term.
                }
                ctxFreq += freq;
            }
        }
        LOG.trace("Context Frequency (Promiscuity) for "+sf+"="+ctxFreq);
        return ctxFreq;
    }


    public long getFrequency(SurfaceForm sf) throws SearchException {
        long ctxFreq = 0;
        Query q = mLucene.getQuery(sf);
        ScoreDoc[] hits = getHits(q);
        for (ScoreDoc d: hits) {
            int docNo = d.doc;
            String[] onlySf = {LuceneManager.DBpediaResourceField.SURFACE_FORM.toString()};
            //Surface Form field does not store frequency vector, but indexes surface forms multiple times.
            Document doc = getDocument(docNo, new MapFieldSelector(onlySf));
            String[] sfList = doc.getValues(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString());

            //Need to filter sfList to keep only the elements that are equal to the sf (caution with lowercase)
            int i=0;
            for (String sfName: sfList) {
                if (sfName.toLowerCase().equals(sf.name().toLowerCase())) //TODO un-hardcode lowercase! :)
                    ctxFreq++;
                i++;
            }
        }
        LOG.trace("Frequency for "+sf+"="+ctxFreq);
        return ctxFreq;
    }

    public List<Document> getDocuments(SurfaceForm sf, FieldSelector fieldSelector) throws SearchException {
        //LOG.trace("Retrieving documents for surface form: "+res);

        // search index for surface form
        List<Document> documents = new ArrayList<Document>();

        // Iterate through the results:
        for (ScoreDoc hit : getHits(mLucene.getQuery(sf))) {
            documents.add(getDocument(hit.doc, fieldSelector));
        }
        //LOG.debug(documents.size()+" documents found.");

        // return set of surrogates
        return documents;
    }

    //TODO make this configurable!
    int minContextWords = 0;
    int maxContextWords = 500;
    ContextExtractor contextExtractor = new ContextExtractor(minContextWords, maxContextWords);

    public ScoreDoc[] getHits(SurfaceFormOccurrence sfOcc) throws SearchException, InputException {
        Text narrowContext = contextExtractor.narrowContext(sfOcc).context();
        ScoreDoc[] hits = getHits(mLucene.getQuery(sfOcc.surfaceForm(), narrowContext));
        return hits;
    }
    public ScoreDoc[] getHitsSurfaceFormHack(SurfaceFormOccurrence sfOcc, SurfaceForm hackedSf) throws SearchException, InputException { //TODO this hack attempts to null the effect of another hack that disappears with determiners at index time
        Text narrowContext = contextExtractor.narrowContext(sfOcc).context();
        ScoreDoc[] hits = getHits(mLucene.getQuery(hackedSf, narrowContext));
        return hits;
    }

    public ScoreDoc[] getHits(DBpediaResource resource) throws SearchException {
        return getHits(mLucene.getQuery(resource), 2); // only needs one, but get two just to check if index is corrupted
    }

    public int getAmbiguity(SurfaceForm sf) throws SearchException { //HACK surface form (to be solved with analyzers in getQuery(SurfaceForm)
        ScoreDoc[] hits = getHits(mLucene.getQuery(sf));
        String sfName = sf.name().trim();
        if (hits.length == 0) { //HACK surface form (to be solved with analyzers in getQuery(SurfaceForm)
            if (sfName.toLowerCase().startsWith("the ")) {
                String newName = sfName.substring(3).trim();
                hits = getHits(mLucene.getQuery(new SurfaceForm(newName)));
                LOG.debug("New sfName="+newName+" hits="+hits.length);
            } else if (sfName.toLowerCase().startsWith("a ")) {
                String newName = sfName.substring(1).trim();
                hits = getHits(mLucene.getQuery(new SurfaceForm(newName)));
                LOG.debug("New sfName="+newName+" hits="+hits.length);
            }
        }
        if (hits.length == 0 && sfName.toLowerCase().endsWith("s")) {
            String newName = sfName.substring(0,sfName.length()-1).trim();
            hits = getHits(mLucene.getQuery(new SurfaceForm(newName)));
            LOG.debug("New sfName="+newName+" hits="+hits.length);
        }
        LOG.trace("Ambiguity for "+sf+"="+hits.length);
        return hits.length;
    }

    public Set<DBpediaResource> getCandidates(SurfaceForm sf) throws SearchException {
        return getDBpediaResources(sf);
    }

    public Set<DBpediaResource> getDBpediaResources(SurfaceForm sf) throws SearchException {
        Set<DBpediaResource> results = new HashSet<DBpediaResource>();
        ScoreDoc[] hits = getHits(mLucene.getQuery(sf));
        for (ScoreDoc sd: hits) {
            results.add(getDBpediaResource(sd.doc));
        }
        LOG.trace("Ambiguity for "+sf+"="+hits.length);
        return results;
    }

    /**
     * Gets number of occurrences (support) for DBpediaResource.
     * Will issue a search. If you have a document, prefer using getSupport(Document).
     * @param res
     * @return
     * @throws SearchException
     */
    public int getSupport(DBpediaResource res) throws SearchException {
        FieldSelector fieldSelector = new MapFieldSelector(uriAndCount);
        int support = 0;

        //if (res.support()>0) return res.support(); //TODO what happens if value is already set?

        List<Document> uris = getDocuments(res, fieldSelector);
        if (uris.size()>1)
            LOG.error("Found the same URI twice in the index: "+res);

        for (Document doc: uris) { //TODO should only return one.
            String value = doc.get(LuceneManager.DBpediaResourceField.URI_COUNT.toString());
            if (value==null) { //backwards compatibility
                support = doc.getValues(LuceneManager.DBpediaResourceField.URI.toString()).length;
            } else {
                support = new Integer(value);
            }
        }
        return support;
    }


    /**
     * Returns the number of URIs in document number docNo:
     * Represents the number of times the URI was seen in the training data
     *
     * @param document
     * @return
     * @throws SearchException
     */
    public int getSupport(Document document) throws SearchException {
        String fieldValue = document.get(LuceneManager.DBpediaResourceField.URI_COUNT.toString());

        int support = 0;
        if (fieldValue==null) { // backwards compatibility
            support = document.getFields(LuceneManager.DBpediaResourceField.URI.toString()).length;  // number of URI fields in this document;
        } else {
            support = new Integer(fieldValue); // from URI_COUNT field
        }

        return support;
    }

    // Returns a list of DBpediaTypes that are registered in the index in document number docNo.
    // Duplicates are not removed.
    // CAUTION: sorting is not guaranteed! (but should be fine (Max thinks) if an order was given when indexing (typically from least to most specific)
    public List<DBpediaType> getDBpediaTypes(Document document) throws SearchException {
        String[] types = document.getValues(LuceneManager.DBpediaResourceField.TYPE.toString());
        List<DBpediaType> typesList = new ArrayList<DBpediaType>();
        for (String t : types) {
            typesList.add(new DBpediaType(t));
        }
        return typesList;
    }

    /**
     *
     * @param dbpediaResource
     * @return map from term (word) to count
     */
    public List<Map.Entry<String,Integer>> getContextWords(DBpediaResource dbpediaResource) throws SearchException {
        Map<String,Integer> termFreqMap = new HashMap<String,Integer>();
        ScoreDoc[] docs = getHits(dbpediaResource);
        //TODO Create an exception DuplicateResourceException
        if (docs.length>1)
            LOG.error(String.format("Resource %s has more than one document in  the index. Maybe index corrupted?", dbpediaResource));
        // Will accept multiple docs for a resource and get the overall top terms
        try {
            for (ScoreDoc d: docs) {
                TermFreqVector vector = mReader.getTermFreqVector(d.doc, LuceneManager.DBpediaResourceField.CONTEXT.toString());
                if (vector==null)
                    throw new SearchException("The index you are using does not have term frequencies stored. Cannot run getContextWords(DBpediaResource).");
                int[] freqs = vector.getTermFrequencies();
                String[] terms = vector.getTerms();
                for (int i=0; i < vector.size(); i++) {
                    termFreqMap.put(terms[i],freqs[i]);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        Ordering descOrder = new Ordering<Map.Entry<String,Integer>>() {
            public int compare(Map.Entry<String,Integer> left, Map.Entry<String,Integer> right) {
                return Ints.compare(right.getValue(), left.getValue());
            }
        };
        List<Map.Entry<String,Integer>> sorted = descOrder.sortedCopy(termFreqMap.entrySet());

        return sorted;
    }



    @Override
    public List<FeatureVector> get(DBpediaResource resource) throws SearchException {
        List<FeatureVector> vectors = new ArrayList<FeatureVector>();

        // Iterate through the results:
        for (ScoreDoc hit : getHits(mLucene.getQuery(resource))) {
            int docNo = hit.doc;
            TermFreqVector vector = getVector(docNo);
            vectors.add(new LuceneFeatureVector(resource, vector));
        }
        LOG.trace(vectors.size()+" vectors found.");

        // return set of vectors
        return vectors;
    }



    /**
     * Generates explanations for how a given SurfaceFormOccurrence has been disambiguated into a DBpediaResourceOccurrence
     * @param goldStandardOccurrence
     * @param nExplanations
     * @return a list of explanations
     * @throws SearchException
     */
    public List<Explanation> explain(DBpediaResourceOccurrence goldStandardOccurrence, int nExplanations) throws SearchException {
        List<Explanation> explanations = new ArrayList<Explanation>();
        int i = 0;
        // We know that the disambiguate should have found the URI in goldStandardOccurrence
        int shouldHaveFound = getHits(goldStandardOccurrence.resource())[0].doc;
        LOG.debug("Explanation: ");
        LOG.debug("\t"+goldStandardOccurrence.resource()+" has doc no. = "+shouldHaveFound);
        //Search the index for the surface form in goldStandardOccurrence
        //for (ScoreDoc hit : getHits() { // query for a surface form
            Query actuallyFound = mLucene.getQuery(goldStandardOccurrence.surfaceForm(), goldStandardOccurrence.context()); // for each of the vectors found, we'll give an explanation
            try {
                LOG.debug("\tSurface Form Occurrence looks like: "+actuallyFound.toString());
                //Returns an Explanation that describes how doc(actuallyFound) scored against weight/query (shouldHaveFound).
                explanations.add(mSearcher.explain(actuallyFound, shouldHaveFound));
            } catch (IOException e) {
                throw new SearchException("Error generating explanation for occurrence "+goldStandardOccurrence, e);
            }
            i++;
            //if (i >= nExplanations) break;
        //}
        return explanations;
    }

    public long getNumberOfResources() throws IOException {
        return mSearcher.maxDoc();
//        long total = 0;
//        try {
//            total = mSearcher.maxDoc();
//        } catch (IOException e) {
//            LOG.error("Error reading number of resources. "+e.getMessage());
//        }
//        return total;
    }


    public double getAverageIdf(Text context) throws IOException {
        double idfSum = 0;
        int wordCount = 0;

        Set<String> seenWords = new HashSet<String>();

//        StringReader sr = new StringReader(context.text());
//        TokenStream ts = mLucene.defaultAnalyzer().tokenStream(LuceneManager.DBpediaResourceField.CONTEXT.toString(), sr);
//        Token t;
//        while((t = ts.next()) != null) {
//            String word = t.term();

        for(String word : context.text().split("\\W+")) {   // simple tokenizer
            if(seenWords.contains(word) || word.trim().equals("")) {
                continue;
            }

            try {
                int docCount = getHits(mLucene.getQuery(new Text(word))).length;

    //            TermDocs tds = mReader.termDocs(new Term(t.term(), LuceneManager.DBpediaResourceField.CONTEXT.toString()););
    //            int docCount = 0;
    //            while(tds.next()) {
    //                docCount++;
    //            }

                if(docCount > 0) {
                    wordCount++;
                    idfSum += Math.log(numDocs/docCount);
                }

            } catch (SearchException e) {
                LOG.trace("word "+word+"not found in context field when getting average idf");
            }
            seenWords.add(word);
        }

        return idfSum/wordCount;
    }

}
