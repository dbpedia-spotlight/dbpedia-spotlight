package org.dbpedia.spotlight.lucene.search;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.*;
import org.dbpedia.spotlight.lucene.similarity.CachedSimilarity;

import java.io.IOException;

/**
 * This class extends TermQuery so that we can take two terms (surface form term, context term)
 *
 * This fundamentally changes how we perform
 *  public Query getQuery(SurfaceForm sf, Text context)
 *
 *
 * @author pablomendes
 */
public class CandidateResourceQuery extends TermQuery {

    private Term term; // this is private in TermQuery, will have to use our own.
    private Term surfaceFormTerm;

    /**
     * Constructs a query for the context term <code>t</code>, given surface form surfaceFormTerm
     */
    public CandidateResourceQuery(Term surfaceFormTerm, Term t) {
        super(t);
        this.surfaceFormTerm = surfaceFormTerm;
    }

    private class CandidateResourceWeight extends Weight {

        private Similarity similarity;
        private float value;
        private float idf;
        private float queryNorm;
        private float queryWeight;
        private Explanation.IDFExplanation idfExp;

        public CandidateResourceWeight(Searcher searcher) throws IOException {
            this.similarity = getSimilarity(searcher);

            if (this.similarity instanceof CachedSimilarity) // this guy has a pointer to the cache and can benefit from explicit surface form
                idfExp = ((CachedSimilarity) similarity).idfExplain(surfaceFormTerm, term, searcher);
            else {
                idfExp = similarity.idfExplain(term, searcher); // otherwise it will have to decide inside which term is the surface form
            }
            idf = idfExp.getIdf();
        }

        public String toString() { return "weight(" + CandidateResourceQuery.this + ")"; } //SORRY copy+paste because TermWeight is private

        public Query getQuery() { return CandidateResourceQuery.this; } //SORRY copy+paste because TermWeight is private

        public float getValue() { return value; } //SORRY copy+paste because TermWeight is private

        public float sumOfSquaredWeights() { //SORRY copy+paste because TermWeight is private
            queryWeight = idf * getBoost();             // compute query weight
            return queryWeight * queryWeight;           // square it
        }

        public void normalize(float queryNorm) { //SORRY copy+paste because TermWeight is private
            this.queryNorm = queryNorm;
            queryWeight *= queryNorm;                   // normalize query weight
            value = queryWeight * idf;                  // idf for document
        }

        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException { //SORRY copy+paste because TermWeight is private
            TermDocs termDocs = reader.termDocs(term);

            if (termDocs == null)
                return null;

            return new TermScorer(this, termDocs, similarity, reader.norms(term.field()));
        }

        public Explanation explain(IndexReader reader, int doc)   //SORRY copy+paste because TermWeight is private
                throws IOException {

            ComplexExplanation result = new ComplexExplanation();
            result.setDescription("weight("+getQuery()+" in "+doc+"), product of:");

            Explanation expl = new Explanation(idf, idfExp.explain());

            // explain query weight
            Explanation queryExpl = new Explanation();
            queryExpl.setDescription("queryWeight(" + getQuery() + "), product of:");

            Explanation boostExpl = new Explanation(getBoost(), "boost");
            if (getBoost() != 1.0f)
                queryExpl.addDetail(boostExpl);
            queryExpl.addDetail(expl);

            Explanation queryNormExpl = new Explanation(queryNorm,"queryNorm");
            queryExpl.addDetail(queryNormExpl);

            queryExpl.setValue(boostExpl.getValue() *
                    expl.getValue() *
                    queryNormExpl.getValue());

            result.addDetail(queryExpl);

            // explain field weight
            String field = term.field();
            ComplexExplanation fieldExpl = new ComplexExplanation();
            fieldExpl.setDescription("fieldWeight("+term+" in "+doc+
                    "), product of:");

            Explanation tfExpl = scorer(reader, true, false).explain(doc);
            fieldExpl.addDetail(tfExpl);
            fieldExpl.addDetail(expl);

            Explanation fieldNormExpl = new Explanation();
            byte[] fieldNorms = reader.norms(field);
            float fieldNorm =
                    fieldNorms!=null ? Similarity.decodeNorm(fieldNorms[doc]) : 1.0f;
            fieldNormExpl.setValue(fieldNorm);
            fieldNormExpl.setDescription("fieldNorm(field="+field+", doc="+doc+")");
            fieldExpl.addDetail(fieldNormExpl);

            fieldExpl.setMatch(Boolean.valueOf(tfExpl.isMatch()));
            fieldExpl.setValue(tfExpl.getValue() *
                    expl.getValue() *
                    fieldNormExpl.getValue());

            result.addDetail(fieldExpl);
            result.setMatch(fieldExpl.getMatch());

            // combine them
            result.setValue(queryExpl.getValue() * fieldExpl.getValue());

            if (queryExpl.getValue() == 1.0f)
                return fieldExpl;

            return result;
        }
    }

}
