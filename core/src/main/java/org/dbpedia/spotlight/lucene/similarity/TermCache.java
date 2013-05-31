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
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.OpenBitSet;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.Text;

import java.io.IOException;
import java.util.*;

/**
 *
 * The strategy here is to keep as many cached bitsets as possible,
  * @author pablomendes
 */
public abstract class TermCache {

    Log LOG = LogFactory.getLog(TermCache.class);

    long mMaxCacheSize;
    LuceneManager mLuceneManager;

    public TermCache(LuceneManager mgr, long maxCacheSize) {
        setMaxCacheSize(maxCacheSize);
        this.mLuceneManager = mgr;
    }

    public TermCache(IndexReader reader) throws IOException {
        setMaxCacheSize(reader.getUniqueTermCount());
    }


    /**
     * It is faster to do a get(term)!=null, because in our use case we always need to get when checking if contains.
     * @param term
     * @return
     */
    @Deprecated
    public abstract boolean containsKey(Term term);

    public abstract OpenBitSet put(Term term, OpenBitSet openBitSet);

    public abstract OpenBitSet get(Term term);


    /**
     * Retrieves from the cache the DocIdSet for documents containing the context term at hand.
     * If DocIdSet is not cached, creates it by reading from the index (createDocIdSet).
     * @param reader
     * @param term
     * @return
     * @throws java.io.IOException
     * @author pablomendes
     */
    private DocIdSet getDocIdSet(IndexReader reader, Term term) throws IOException
    {
        OpenBitSet contextTermDocIdSet = get(term);
        if (contextTermDocIdSet == null) {
            contextTermDocIdSet = createDocIdSet(reader, term);
            put(term, contextTermDocIdSet);
        }
        OpenBitSet result = (OpenBitSet) contextTermDocIdSet.clone(); // if the outside process modifies it, our copy remains intact
        return result;
    }

   /* (non-Javadoc)
    * adapted from org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.IndexReader)
    * //TODO is there a faster way to do this?
    */
    public OpenBitSet createDocIdSet(IndexReader reader, Term term) throws IOException
    {
        OpenBitSet result=new OpenBitSet(reader.maxDoc());
        TermDocs td = reader.termDocs();
        int c = 0;
        td.seek(term);
        while (td.next())
        {
            c++;
            result.set(td.doc());
        }
        return result;
    }

    public long cardinality(IndexReader reader, Term surfaceFormTerm) throws IOException {
        OpenBitSet surfaceFormDocIdSet = (OpenBitSet) getDocIdSet(reader, surfaceFormTerm);
        return surfaceFormDocIdSet.cardinality();
    }

    /**
     * Returns the number of documents containing the surface form contextTerm AND the contextTerm provided.
     * @param reader
     * @param contextTerm
     * @return
     * @throws java.io.IOException
     */
    public long cardinality(IndexReader reader, Term surfaceFormTerm, Term contextTerm) throws IOException {
        OpenBitSet contextDocIdSet = (OpenBitSet) getDocIdSet(reader, contextTerm);
        OpenBitSet surfaceFormDocIdSet = (OpenBitSet) getDocIdSet(reader, surfaceFormTerm);
        contextDocIdSet.and(surfaceFormDocIdSet);
        return contextDocIdSet.cardinality();
    }

    /**
     * Returns the number of documents containing the all terms in the set provided
     * @param reader
     * @param terms
     * @return
     * @throws java.io.IOException
     */
    public long cardinality(IndexReader reader, Set<Term> terms) throws IOException {
        long c = 0;
        OpenBitSet result = null;
        for (Term t: terms) {
            OpenBitSet bs = (OpenBitSet) getDocIdSet(reader, t);
            if (result==null)
                result = bs;
            else
                result.and(bs);
        }
        if (result!=null) c = result.cardinality();
        return c;
    }

    public long getMaxCacheSize() {
        return mMaxCacheSize;
    }

    public void setMaxCacheSize(long maxCacheSize) {
        this.mMaxCacheSize = maxCacheSize;
        LOG.info(String.format("Setting the SurrogateCache.maxCacheSize to %s", maxCacheSize));
    }

    /**
     * How many times does a surface form occur in the context field?
     * For phrases, only approximation is possible.
     * @param reader
     * @param sf
     * @return
     */
    public long getPromiscuity(IndexReader reader, SurfaceForm sf) {
        long p = 0;
        Query q;
        try {
            // Use query parser to get analyzed terms
            q = mLuceneManager.getQuery(new Text(sf.name()));
            Set<Term> terms = new HashSet<Term>();
            q.extractTerms(terms);
            LOG.info(String.format("Terms: %s",terms));
            // Now get how many documents contain all of those terms
            p = cardinality(reader, terms);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return p;
    }

}
