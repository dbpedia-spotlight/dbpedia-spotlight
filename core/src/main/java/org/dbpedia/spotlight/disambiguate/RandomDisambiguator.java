package org.dbpedia.spotlight.disambiguate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.model.*;

import java.util.*;

/**
 * Randomly picks one of the candidate URIs for a surface form. Used in evaluation as a baseline.
 * Uses an approximate Gaussian distribution
 *
 * @author pablomendes
 */
public class RandomDisambiguator extends CustomScoresDisambiguator {

    Log LOG = LogFactory.getLog(this.getClass());
    RandomGaussian gaussian = new RandomGaussian();

    public RandomDisambiguator(CandidateSearcher surrogates) {
        super(surrogates, null);
    }

    /**
     * Overrides the CustomScoresDisambiguator.getScores method in order to generate random scores instead of getting from a map.
     * @param candidates
     * @return
     */
    protected List<DBpediaResourceOccurrence> getScores(SurfaceFormOccurrence sfOccurrence, Set<DBpediaResource> candidates) {
        List<DBpediaResourceOccurrence> occurrences = new ArrayList<DBpediaResourceOccurrence>();
        for (DBpediaResource c: candidates) {
            Double score = gaussian.getGaussian();
            DBpediaResourceOccurrence occ = new DBpediaResourceOccurrence(c,
                sfOccurrence.surfaceForm(),
                sfOccurrence.context(),
                sfOccurrence.textOffset(),
                score);
            occurrences.add(occ);
        }
        return occurrences;
    }

    /**
     Generate pseudo-random floating point values, with an
     approximately Gaussian (normal) distribution.

     Many physical measurements have an approximately Gaussian
     distribution; this provides a way of simulating such values.
     http://www.javapractices.com/topic/TopicAction.do?Id=62
     */
    public static final class RandomGaussian {

        double mMean = 100.0f;
        double mVariance = 5.0f;

        private Random fRandom = new Random();

        public RandomGaussian() {}

        public RandomGaussian(double aMean, double aVariance) {
            mMean = aMean;
            mVariance = aVariance;
        }

        private double getGaussian(){
            return mMean + fRandom.nextGaussian() * mVariance;
        }

        private double getGaussian(double aMean, double aVariance){
            return aMean + fRandom.nextGaussian() * aVariance;
        }

    }

    public static void main(String... aArgs){
        RandomGaussian gaussian = new RandomGaussian();
        double MEAN = 100.0f;
        double VARIANCE = 5.0f;
        for (int idx = 1; idx <= 10; ++idx){
            System.out.println(String.format("Generated %s ", gaussian.getGaussian(MEAN, VARIANCE)));
        }
    }

}
