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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.OpenBitSet;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.IOException;

/**
 *
 */
public class InvCandFreqSimilarity extends DefaultSimilarity {


    public InvCandFreqSimilarity() {}

    /*
    These terms have to be here so that they are visible across multiple executions of idfExplain
     */
    Term surfaceFormTerm;
    OpenBitSet surrogateDocIdSet;
    long maxSf = 1;

//    public float tf(float freq) {
//        return (float) (freq>0 ? 1.0 : 0.0);
//    }

    @Override
    public Explanation.IDFExplanation idfExplain(final Term term, final Searcher searcher) throws IOException {
        final int df = searcher.docFreq(term);
        final int max = searcher.maxDoc();
        final float idf = idf(df, max);

        return new Explanation.IDFExplanation() {

            long sf = 0;

            boolean isSurfaceFormField = term.field().equals(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString());

            private long sf() {

                try {
                    IndexReader reader = ((IndexSearcher) searcher).getIndexReader();
                    TermsFilter filter = new TermsFilter();
                    filter.addTerm(term);
                    OpenBitSet it  = (OpenBitSet) filter.getDocIdSet(reader);

                    if(isSurfaceFormField) { // Here we set the surface form specific information.
                        surfaceFormTerm = term;                  // Store the surface form
                        surrogateDocIdSet = (OpenBitSet) it;     // Store what documents are possible surrogates (URIs that can be represented by this surface form(
                        maxSf = surrogateDocIdSet.cardinality(); // This is the number of documents that contain the surface form (size of surrogate set)
                    } else {
                        it.and(surrogateDocIdSet);               // Find what surrogates contain this term in the context
                    }

//                    long maxSurfaceFormFreq = surrogateDocIdSet.cardinality();
//                    long termFreq = it.cardinality();
//                    LOG.trace(term);
//                    LOG.trace("surrogateDocIdSet.cardinality() ="+c1);
//                    LOG.trace("it.cardinality() ="+c2);

                    // If this is a SURFACE_FORM term: number of documents in which the surface form occurred
                    // Else, this is a CONTEXT term: number of docs the term and the surface form occurred
                    sf = ((OpenBitSet)it).cardinality();
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
                return  "isf(docFreq=" + sf +
                        ", maxDocs="+ maxSf + ")";
            }
            @Override
            public float getIdf() {
                sf = sf();
                float isf = isf(sf, maxSf);
                //return 2;
                return isf;
                //return idf * isf;
            }};
    }

}
