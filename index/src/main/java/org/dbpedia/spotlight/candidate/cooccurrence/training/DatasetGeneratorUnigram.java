package org.dbpedia.spotlight.candidate.cooccurrence.training;

import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterPOS;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterTermsize;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.SurfaceForm;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Generates a pre-annotated training data TSV file for unigram term candidates.
 *
 * <p>
 * This is a Java program that can be used to generate pre-annotated training data for the
 * training of a unigram classifier.
 * The TSV file produced by this class must be evaluated and modified by hand and can then
 * be read and enriched by the class {@link AnnotatedDatasetEnricher}.
 * </p>
 *
 * @author Joachim Daiber
 * 
 */

public class DatasetGeneratorUnigram extends DatasetGenerator {

	private static String[] BASIC_EXAMPLES
			= {"brink", "star", "fight", "term", "mind", "role", "concept", "yard", "room", "policy", "lamp"};



	/**
	 * A training data generator for term candidates.
	 *
	 * @throws java.net.UnknownHostException
	 * @throws org.dbpedia.spotlight.exceptions.ConfigurationException
	 *
	 */
	public DatasetGeneratorUnigram() throws UnknownHostException, ConfigurationException {
		super();
	}

	@Override
	protected boolean isValidExampleSentence(AnnotatedSurfaceFormOccurrence exampleSentenceGeneric) {

		System.out.println("Looking at " + exampleSentenceGeneric);

		boolean b = !new FilterPOS().applies(exampleSentenceGeneric);
		System.out.println("Return " + b);


		return b;
	}


	public static void main(String[] args) throws ConfigurationException, SearchException, IOException {

		DatasetGenerator trainingDataGenerator = new DatasetGeneratorUnigram();

		
		/**
		 * Load list of confusables and filter to only include unigrams.
		 */
		
		List<String> confusables = loadConfusables(new File("/Users/jodaiber/Documents/workspace/ba/Bachelor Thesis/01 Evaluation/03 Training/Training Set/data/confusables_with_wiktionary.nt"));

		FilterTermsize filter = new FilterTermsize(FilterTermsize.Termsize.unigram, trainingDataGenerator.getLuceneFactory().textUtil());
		filter.apply(confusables);


		/**
		 * Take NUMBER_OF_EXAMPLES random confusables from the list
		 */
		
		Random random = new Random();
		List<String> exampleConfusables = new ArrayList<String>();
		
		for(int i = 0; i < NUMBER_OF_EXAMINED_SURFACE_FORMS; i++) {
			int r = random.nextInt(confusables.size());
			exampleConfusables.add(confusables.get(r));
			confusables.remove(r);
		}

		exampleConfusables.addAll(Arrays.asList(BASIC_EXAMPLES));

		/**
		 * Gather information for each confusable and write to TSV
		 */
		
		AnnotatedDataset occurrenceTrainingDataset = new AnnotatedDataset();

		for(String exampleConfusable : exampleConfusables) {

			SurfaceForm exampleSF = new SurfaceForm(exampleConfusable);
			occurrenceTrainingDataset.addInstances(trainingDataGenerator.findExampleSentences(exampleSF));

		}
		
		occurrenceTrainingDataset.write(new File("/Users/jodaiber/Desktop/test.tsv"));

	}

	

}
