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
import org.apache.lucene.search.*;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.model.SurfaceForm;

import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Attempting to include surface form belief
 * @author pablomendes
 */
public class NewSimilarity extends DefaultSimilarity implements CachedSimilarity {

    Log LOG = LogFactory.getLog(NewSimilarity.class);

    TermCache termCache;   // Will cache a bitSet for each term in the context
    public NewSimilarity(TermCache cache) {
        termCache = cache;
    }

//    boolean warmUp = false;
//    public CachedInvSenseFreqSimilarity(boolean warmUp) { this.warmUp = true; }

    /*
    These terms have to be here so that they are visible across multiple executions of idfExplain
     */
    Term surfaceFormTerm;
    long maxSf = 1;
    long promiscuity = 1;

//    public float tf(float freq) {
//        return (float) (freq>0 ? 1.0 : 0.0);
//    }

   private float round(float d) {
        float result = d;
        DecimalFormat twoDForm = new DecimalFormat("#.######");
        if (Float.isInfinite(d)) {
            result = Float.MAX_VALUE;
        } else if (Float.isNaN(d)) {
            result = -2;
        } else {
            result = Float.valueOf(twoDForm.format(d));
        }
	    return result;
    }

    public Explanation.IDFExplanation idfExplain(final Term sfTerm, final Term term, final Searcher searcher) throws IOException {
        throw new IOException("Not implemented yet.");
    }

    @Override
    public Explanation.IDFExplanation idfExplain(final Term term, final Searcher searcher) throws IOException {
        //final int df = searcher.docFreq(term);
        //final int max = searcher.maxDoc();
        //final float idf = idf(df, max);


            return new Explanation.IDFExplanation() {

                long sf = 0;

                boolean isSurfaceFormField = term.field().equals(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString());

                private long sf() {

                    try {
                        IndexReader reader = ((IndexSearcher) searcher).getIndexReader();

                        if(isSurfaceFormField) { // Here we set the surface form specific information.
                            surfaceFormTerm = term;                  // Store the surface form
                            maxSf = termCache.cardinality(reader, surfaceFormTerm); // This is the number of documents that contain the surface form (size of surrogate set)
                            sf = maxSf; // setting sf = maxSf generates isf=1, leading to tf*isf = tf
                            promiscuity = termCache.getPromiscuity(reader, new SurfaceForm(term.text()));
                        } else {
                            sf = termCache.cardinality(reader, surfaceFormTerm, term); // This is the number of docs containing sf + term
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

                // How convinced are we that this surface form should be tagged
                public float spotBelief(long maxSenseFreq, long promiscuity) {
                    //return promiscuity==0 ? 0 : (float) (Math.log(new Float(maxSenseFreq) / new Float(promiscuity)) + 1.0);
                    return round(new Float(maxSenseFreq) / new Float(promiscuity));
                }

                public float isf(long senseFreq, long maxSenseFreq) {
                    return senseFreq==0 ? 0 : (float) (Math.log(new Float(maxSenseFreq) / new Float(senseFreq)) + 1.0);
                }

                @Override
                public String explain() {
                    return  "isf(docFreq=" + sf +
                            ", maxDocs="+ maxSf + ")";
                }
                @Override
                public float getIdf() {
                    sf = sf();
                    float isf = isf(sf, maxSf);
                    return isf * spotBelief(maxSf, promiscuity);
                }
            };
    }

    public TermCache getTermCache() {
        return termCache;
    }

    public void setTermCache(TermCache termCache) {
        this.termCache = termCache;
    }
}
