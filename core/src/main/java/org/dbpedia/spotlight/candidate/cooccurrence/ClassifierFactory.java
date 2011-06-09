package org.dbpedia.spotlight.candidate.cooccurrence;

import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassifier;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.exceptions.InitializationException;

/**
 * Factory for candidate classifiers (uni- and n-gram)
 *
 * @author Joachim Daiber
 */
public class ClassifierFactory {

	private static CandidateClassifier classifierUnigram;
	private static CandidateClassifier classifierNGram;

	public ClassifierFactory(String unigramModelFile, String ngramModelFile,
							 String occurrenceDataSource, OccurrenceDataProvider dataProvider)
			throws InitializationException {

		//Create the unigram classifier:
		classifierUnigram = new CandidateClassifier(
				unigramModelFile, dataProvider,
				InstanceBuilderFactory.createInstanceBuilderUnigram(occurrenceDataSource, dataProvider));
		//classifierUnigram.setVerboseMode(true);

		//Create the n-gram classifier:
		classifierNGram = new CandidateClassifier(
				ngramModelFile, dataProvider,
				InstanceBuilderFactory.createInstanceBuilderNGram(occurrenceDataSource, dataProvider));
		//classifierNGram.setVerboseMode(true);

		
	}

	public static CandidateClassifier getClassifierUnigram() {
		return classifierUnigram;
	}

	public static CandidateClassifier getClassifierNGram() {
		return classifierNGram;
	}
	
	
}
