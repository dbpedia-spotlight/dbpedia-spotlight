package org.dbpedia.spotlight.candidate.cooccurrence;

import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClassifier;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.exceptions.InitializationException;

/**
 * Factory for candidate classifiers (unigram and n-gram).
 *
 * @author Joachim Daiber
 */
public class ClassifierFactory {

	private static CandidateClassifier classifierUnigram;
	private static CandidateClassifier classifierNGram;

	
	/**
	 * Create a new ClassfifierFactory with the provided occurrence data provider and model files.
	 *  
	 * @param unigramModelFile path to serialized classifier model file for unigrams
	 * @param ngramModelFile path to serialized classifier model file for n-grams
	 * @param occurrenceDataSource the source of the occurrence data, which will be used
	 * to select the proper {@link org.dbpedia.spotlight.candidate.cooccurrence.weka.InstanceBuilder}.
	 * @param dataProvider occurrence data provider used in the InstanceBuilder.
	 * @throws InitializationException Error during 
	 */
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
	
	
	/**
	 * Retrieve an instance of a unigram classifier.
	 * @return unigram classifier
	 */
	public static CandidateClassifier getClassifierInstanceUnigram() {
		return classifierUnigram;
	}
	

	/**
	 * Retrieve an instance of a ngram classifier.
	 * @return n-gram classifier
	 */
	public static CandidateClassifier getClassifierInstanceNGram() {
		return classifierNGram;
	}
	
}
