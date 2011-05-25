package org.dbpedia.spotlight.candidate.cooccurrence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassifier;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassifierNGram;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassifierUnigram;

/**
 * @author Joachim Daiber
 */
public class CoOccurrenceBasedFactory {

	private final Log LOG = LogFactory.getLog(this.getClass());

	private static CandidateClassifier classifierUnigram;
	private static CandidateClassifier classifierNGram;


	public CoOccurrenceBasedFactory(String modelFileUnigram, String modelFileNGram) {

		try{
			classifierUnigram = new CandidateClassifierUnigram(modelFileUnigram);
		}catch (Exception e) {
			LOG.error("Could not initialize unigram classifier." + e);
		}
		classifierUnigram.setVerboseMode(true);

		try{
			classifierNGram = new CandidateClassifierNGram(modelFileNGram);
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
