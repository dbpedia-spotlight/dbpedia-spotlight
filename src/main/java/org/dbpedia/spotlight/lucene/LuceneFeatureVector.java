package org.dbpedia.spotlight.lucene;

import org.apache.lucene.index.TermFreqVector;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.vsm.FeatureVector;

/**
 * A lucene vector is a surrogate for a dbpedia resource in our vector space model implemented with lucene.
 * TODO Wondering if this is the same as TFVector (note: it uses TermFreqVector which is a Lucene specific class)
 *
 * User: PabloMendes
 * Date: Jun 29, 2010
 * Time: 1:36:45 PM
 */
public class LuceneFeatureVector implements FeatureVector {

    DBpediaResource resource;
    TermFreqVector vector;

    public LuceneFeatureVector(DBpediaResource resource, TermFreqVector vector) {
        this.resource = resource;
        this.vector = vector;
    }

    public DBpediaResource resource() {
        return resource;
    }

    public TermFreqVector vector() {
        return vector;        
    }

    @Override
    public String toString() {
        return "LuceneFeatureVector["+
                this.resource.uri()+": "+
                this.vector.toString()+"]";
    }
}
