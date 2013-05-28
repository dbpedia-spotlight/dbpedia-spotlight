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

package org.dbpedia.spotlight.lucene.similarity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.IOException;

/**
 * ICF - Inverse Candidate Frequency
 * Caching -> Attempting to optimize time performance, since some context terms are likely to occur with many surface forms.
 *
 * @author pablomendes
 */
public class CachedInvCandFreqSimilarity extends DefaultSimilarity implements CachedSimilarity {

    Log LOG = LogFactory.getLog(CachedInvCandFreqSimilarity.class);

    TermCache termCache;   // Will cache a bitSet for each term in the context
    public CachedInvCandFreqSimilarity(TermCache cache) {
        termCache = cache;
    }

//    boolean warmUp = false;
//    public CachedInvSenseFreqSimilarity(boolean warmUp) { this.warmUp = true; }

    /*
    These terms have to be here so that they are visible across multiple executions of idfExplain  (NOT THREAD SAFE?)
     */
    Term surfaceFormTerm;
    long maxSf = 1;
    float surfaceFormIDF = 1;

//    public float tf(float freq) {
//        return (float) (freq>0 ? 1.0 : 0.0);
//    }

    public Explanation.IDFExplanation idfExplain(final Term surfaceFormTerm, final Term term, final Searcher searcher) throws IOException {
        final int df = searcher.docFreq(term);
        final int max = searcher.maxDoc();
        final float idf = idf(df, max);
        final long maxCf = termCache.cardinality(((IndexSearcher) searcher).getIndexReader(), surfaceFormTerm); // This is the number of documents that contain the surface form (size of candidate set)
        float surfaceFormIDF = idf(df, max);

        return new Explanation.IDFExplanation() {

            long cf = 0; // candidate frequency: with how many candidate resources this context term appears

            private long cf() {

                IndexReader reader = ((IndexSearcher) searcher).getIndexReader();

                try {
                        //if (surfaceFormTerm==null) {  //TODO may not need this anymore
                        //    cf = termCache.cardinality(reader, term); // This is the number of docs containing term
                        //    LOG.debug("Surface form is null:"+term);
                        //} else {
                            cf = termCache.cardinality(reader, surfaceFormTerm, term); // This is the number of docs containing surface form + term (candidates)
                        //}

                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

                return cf;
            }

            /**
             * Inverse Candidate Frequency calculation
             * @param candidateFreq
             * @param maxCandidateFreq
             * @return
             */
            public float icf(long candidateFreq, long maxCandidateFreq) {
                return candidateFreq==0 ? 0 : (float) (Math.log(new Float(maxCandidateFreq) / new Float(candidateFreq)) + 1.0);
            }

            @Override
            public String explain() {
                    return  "idf(docFreq=" + df +
                            ", maxDocs="+ max + ")" +
                            "icf(docFreq=" + cf +
                            ", maxDocs="+ maxCf + ")";
            }

            @Override
            public float getIdf() {
                    cf = cf(); // sense frequency
                    float icf = icf(cf, maxCf); // inverse sense frequency
                    return icf;
            }};

    }

    //FIXME this is not thread-safe
    @Override
    public Explanation.IDFExplanation idfExplain(final Term term, final Searcher searcher) throws IOException {
        final int df = searcher.docFreq(term);   //TODO for URI term, the df is the value of the field uriCount ?
        final int max = searcher.maxDoc();
        final float idf = idf(df, max);

        return new Explanation.IDFExplanation() {

            long sf = 0;

            boolean isSurfaceFormField = term.field().equals(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString());
            //TODO can optimize for URI term. No need to compute anything, just return.
            private long sf() {

                try {
                    IndexReader reader = ((IndexSearcher) searcher).getIndexReader();

                    if(isSurfaceFormField) { // Here we set the surface form specific information.
                        surfaceFormTerm = term;                  // Store the surface form
                        maxSf = termCache.cardinality(reader, surfaceFormTerm); // This is the number of documents that contain the surface form (size of surrogate set)
                        sf = maxSf;
                        surfaceFormIDF = idf;
                    } else {
                        if (surfaceFormTerm==null) {
                            sf = termCache.cardinality(reader, term); // This is the number of docs containing term
                            LOG.trace("[OLD] Surface form is null:"+term); // this can happen for queries only on context field (e.g. for warmUp)
                        } else {
                            sf = termCache.cardinality(reader, surfaceFormTerm, term); // This is the number of docs containing sf + term
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

                return sf;
            }

//            /** Implemented as <code>log(numDocs/(docFreq+1)) + 1</code>. */
//            public float idf(int docFreq, int numDocs) {
//                return (float)(Math.log(numDocs/(double)(docFreq+1)) + 1.0);
//            }

            public float isf(long senseFreq, long maxSenseFreq) {
                return senseFreq==0 ? 0 : (float) (Math.log(new Float(maxSenseFreq) / new Float(senseFreq)) + 1.0);
            }

            @Override
            public String explain() {
                if (isSurfaceFormField) {
                    return  "idf(docFreq=" + df +
                            ", maxDocs="+ max + ")";
                } else {
                    return  "icf(docFreq=" + sf +
                            ", maxDocs="+ maxSf + ")";
                }

            }
            @Override
            public float getIdf() {
//                if (isSurfaceFormField)
//                    return idf; // inverse document frequency
//                else {
                    sf = sf(); // sense frequency
                    float isf = isf(sf, maxSf); // inverse sense frequency
                    return isf;
//                }

            }};
    }

    @Override
    public TermCache getTermCache() {
        return termCache;
    }

    @Override
    public void setTermCache(TermCache termCache) {
        this.termCache = termCache;
    }
}
