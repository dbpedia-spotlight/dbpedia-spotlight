package org.dbpedia.spotlight.lucene.disambiguate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDoc;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher;
import org.dbpedia.spotlight.model.*;

import java.io.IOException;
import java.util.*;

public class MergedOccurrencesDisambiguator implements Disambiguator {

    final Log LOG = LogFactory.getLog(this.getClass());

    MergedOccurrencesContextSearcher mMergedSearcher;

    public MergedOccurrencesDisambiguator(MergedOccurrencesContextSearcher searcher) throws IOException {
        this.mMergedSearcher = searcher;
    }
      // Not used anymore because we always pick the top
//    public MergedOccurrencesDisambiguator(MergedOccurrencesContextSearcher searcher, RankingStrategy strategy) throws IOException {
//        this.mMergedSearcher = searcher;
//        this.mStrategy = strategy;
//    }

    /*
      IDF of surface form
      */
    public Double importance(SurfaceForm sf) throws SearchException {
        long freqUnannotated = this.mMergedSearcher.getConceptNeighborhoodCount(sf);
        long totalDocs = 1;
        try {
            totalDocs = this.mMergedSearcher.getNumberOfResources();
        } catch (IOException e) {
            throw new SearchException("Error getting count of resources. ", e);
        }
        //long freqAnnotated = this.mMergedSearcher.getAmbiguity(sf);
        double prob = (freqUnannotated == 0) ?  0.0 : Math.log(totalDocs / (1+freqUnannotated)); //TODO throw exception: freqUnannotated==0 should not happen!
        //LOG.info(String.format("FreqAnnotated %s; FreqUnnanotated %s; Prob: %s", freqAnnotated, freqUnannotated, prob));
        LOG.info(String.format("FreqAnnotated %s; FreqUnnanotated %s; Prob: %s", 1, freqUnannotated, prob));
        return prob;
    }


    /*

      */
    public List<SurfaceFormOccurrence> spotProbability(List<SurfaceFormOccurrence> spotted) throws SearchException {
        List<SurfaceFormOccurrence> result = new ArrayList<SurfaceFormOccurrence>();
        for (SurfaceFormOccurrence spot: spotted) {
            Double p = spotProbability(spot.surfaceForm());
            //spot.spotProb = p;
            result.add(new SurfaceFormOccurrence(spot.surfaceForm(), spot.context(), spot.textOffset(), spot.provenance(), p)); //FIXME create setter
        }
        return spotted;    
    }

    public Double spotProbability(SurfaceForm sf) throws SearchException {
        long freqUnannotated = this.mMergedSearcher.getContextFrequency(sf);
        long freqAnnotated = this.mMergedSearcher.getFrequency(sf);
        double prob = (freqUnannotated == 0) ?  0.0 : new Double(freqAnnotated) / freqUnannotated;
        LOG.info(String.format("%s; FreqAnnotated %s; FreqUnnanotated %s; Prob: %s", sf, freqAnnotated, freqUnannotated, prob));
        return prob;
    }


    public DBpediaResourceOccurrence disambiguate(SurfaceFormOccurrence sfOcc) throws SearchException, ItemNotFoundException, InputException {
        //LOG.debug("Disambiguating occurrence: "+sfOcc);

        // search index for surface form
        LOG.info("Getting hits for "+sfOcc.surfaceForm()+" and the context...");
        ScoreDoc[] hits = mMergedSearcher.getHits(sfOcc);
 
        if (hits.length == 0)
            throw new ItemNotFoundException("Not found in index: "+sfOcc);

        DBpediaResource resource = mMergedSearcher.getDBpediaResource(hits[0].doc);

        if (resource==null)
            throw new ItemNotFoundException("Could not choose a URI for "+sfOcc.surfaceForm());

        //LOG.info("  found "+hits.length+" surrogates for "+sfOcc.surfaceForm());

        Double score = new Double(hits[0].score);
        Double percentageOfSecond = new Double(-1);
        if (hits.length > 1) {
            percentageOfSecond = hits[1].score / score;
        }

        return new DBpediaResourceOccurrence(
                "",                          // ID
                resource,                    // DBpedia resource
                sfOcc.surfaceForm(),         // surface form
                sfOcc.context(),             // context in which entity occurs
                sfOcc.textOffset(),          // position in the context where surface form occurs
                Provenance.Annotation(),     // value to signal that this occurrence was created by the disambiguation method
                score,                       // similarity score of the highest ranked
                percentageOfSecond,
                sfOcc.spotProb());         // percentage of the second ranked entity in relation to the first ranked entity
    }

    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException, InputException {
        LOG.debug("Disambiguating "+sfOccurrence.surfaceForm());

        //TODO test with narrow context to see if it's faster, better, worse
        //ContextExtractor ce = new ContextExtractor();
        //Text narrowContext = ce.narrowContext(sfOccurrence.surfaceForm(), sfOccurrence.context());
        //LOG.trace(String.format("Narrowed from: \n\t %s t0 \n\t %s", sfOccurrence.context(), narrowContext));
        
        // search index for surface form
        ScoreDoc[] hits = mMergedSearcher.getHits(sfOccurrence);

        if (hits.length == 0)
            throw new ItemNotFoundException("Not found in index: "+sfOccurrence);

        // Loop through all hits, build a map from URI to score
        List<DBpediaResourceOccurrence> rankedOccs = new LinkedList<DBpediaResourceOccurrence>();
        for (int i=0; i < hits.length && i < k; i++) {
            DBpediaResource resource = mMergedSearcher.getDBpediaResource(hits[i].doc);
            //resource can be null! not handled here
            //if (resource==null)
            //    throw new ItemNotFoundException("Could not choose a URI for "+sfOcc.surfaceForm());
            
            Double score = new Double(hits[i].score);
            Double percentageOfSecond = new Double(-1);
            if (hits.length > i+1) {
                percentageOfSecond = hits[i+1].score / score;
            }
            DBpediaResourceOccurrence resultOcc = new DBpediaResourceOccurrence("",
                                                                                resource,
                                                                                sfOccurrence.surfaceForm(),
                                                                                sfOccurrence.context(),
                                                                                sfOccurrence.textOffset(),
                                                                                Provenance.Annotation(),
                                                                                score,
                                                                                percentageOfSecond,
                                                                                sfOccurrence.spotProb());
            rankedOccs.add(resultOcc);
        }

        return rankedOccs;
    }

    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> sfOccs) throws InputException {
        long startTime = System.currentTimeMillis();

        List<DBpediaResourceOccurrence> results = new LinkedList<DBpediaResourceOccurrence>();

        for (SurfaceFormOccurrence sfOcc : sfOccs) {
            try {
                LOG.info("Disambiguating "+sfOcc.surfaceForm()+" ...");
                results.add(disambiguate(sfOcc));
            } catch (ItemNotFoundException e) {
                LOG.error("Could not disambiguate "+sfOcc.surfaceForm()+": "+e);
            } catch (SearchException e) {
                LOG.error("Could not disambiguate "+sfOcc.surfaceForm()+": "+e);
            }
        }

        double totalSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        LOG.info("Total time of all disambiguations: "+totalSeconds+" s");
        LOG.info("Average time of one disambiguation: "+totalSeconds/sfOccs.size()+" s");  // counts a cache look-up as disambiguation
        
        return results;
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName() + ":" + mMergedSearcher.getSimilarity().getClass().getSimpleName();
    }

    @Override
    public int ambiguity(SurfaceForm sf) throws SearchException {
        return mMergedSearcher.getAmbiguity(sf);
    }

    public int trainingSetSize(DBpediaResource res) throws SearchException {
        int n = 0;
        try {
         n = mMergedSearcher.getNumberOfOccurrences(res);
        } catch (SearchException e) {
            if (!e.getCause().getMessage().equals("read past EOF"))
                throw e;
        }
        return n;
    }

    /**
     * Generates explanations for how a given SurfaceFormOccurrence has been disambiguated into a DBpediaResourceOccurrence
     * @param goldStandardOccurrence
     * @param nExplanations
     * @return a list of explanations
     * @throws SearchException
     */
    public List<Explanation> explain(DBpediaResourceOccurrence goldStandardOccurrence, int nExplanations) throws SearchException {
        return explain(goldStandardOccurrence, nExplanations);
    }

}
