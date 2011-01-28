package org.dbpedia.spotlight.lucene.similarity;

/**
 * Interface for Similarity classes that use our custom term cache
 *
 * @author pablomendes
 */
public interface CachedSimilarity {

    public TermCache getTermCache();

    public void setTermCache(TermCache termCache);
}
