package org.dbpedia.spotlight.lucene.similarity;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Searcher;

import java.io.IOException;

/**
 * Interface for Similarity classes that use our custom term cache
 *
 * @author pablomendes
 */
public interface CachedSimilarity {

    public TermCache getTermCache();

    public void setTermCache(TermCache termCache);

    public Explanation.IDFExplanation idfExplain(final Term surfaceFormTerm, final Term term, final Searcher searcher) throws IOException;
}
