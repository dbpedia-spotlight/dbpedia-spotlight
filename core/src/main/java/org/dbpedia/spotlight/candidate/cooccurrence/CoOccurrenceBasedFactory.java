package org.dbpedia.spotlight.candidate.cooccurrence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassifier;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassifierNGram;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassifierUnigram;
import org.dbpedia.spotlight.model.SpotlightConfiguration;

/**
 * @author Joachim Daiber
 */
public class CoOccurrenceBasedFactory {

	private final Log LOG = LogFactory.getLog(this.getClass());

	private static CandidateClassifier classifierUnigram;
	private static CandidateClassifier classifierNGram;


	public CoOccurrenceBasedFactory(SpotlightConfiguration config) {

		try{
			classifierUnigram = new CandidateClassifierUnigram(config.getCandidateClassifierUnigram(), config);
		}catch (Exception e) {
			LOG.error("Could not initialize unigram classifier." + e);
		}
		classifierUnigram.setVerboseMode(true);

		try{
			classifierNGram = new CandidateClassifierNGram(config.getCandidateClassifierNGram(), config);
		}catch (Exception e) {
			LOG.error("Could not initialize ngram classifier." + e);
		}
		classifierNGram.setVerboseMode(true);

	}

	public static CandidateClassifier getClassifierUnigram() {
		return classifierUnigram;
	}

	public static CandidateClassifier getClassifierNGram() {
		return classifierNGram;
	}
}
