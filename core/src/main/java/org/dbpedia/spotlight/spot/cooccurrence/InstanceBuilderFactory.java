package org.dbpedia.spotlight.spot.cooccurrence;

import org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.spot.cooccurrence.weka.*;
import org.dbpedia.spotlight.spot.cooccurrence.weka.googlengram.InstanceBuilderNGramGoogle;
import org.dbpedia.spotlight.spot.cooccurrence.weka.googlengram.InstanceBuilderUnigramGoogle;
import org.dbpedia.spotlight.spot.cooccurrence.weka.ukwac.InstanceBuilderNGramUKWAC;
import org.dbpedia.spotlight.spot.cooccurrence.weka.ukwac.InstanceBuilderUnigramUKWAC;
import org.dbpedia.spotlight.exceptions.InitializationException;

/**
 * <p>
 * Factory for an {@link InstanceBuilder}, which is used for building a WEKA instance
 * for a spotted surface form occurrence.
 * </p>
 *
 * <p>
 * There are builders for unigram and n-gram (n>1) occurrences and the builders
 * must be specific to the occurrence data source used.
 * </p>
 *
 * <p>
 * To add a new data source, add a configuration id for your data source to both builder
 * methods and create instances of your custom {@link InstanceBuilder}s in
 * {@link org.dbpedia.spotlight.spot.cooccurrence.weka}
 * </p>
 *
 * @author Joachim Daiber
 */

public class InstanceBuilderFactory {


	/**
	 * The configuration ids of the possible data sources:
	 */
	private static final String UKWAC = "ukwac";
	private static final String GOOGLE_NGRAM = "google";
	

	/**
	 * Creates an instance builder for unigrams (based on the specified data source).
	 *
	 * @param dataSource String id of the data source read from the configuration file
	 * @param dataProvider the occurrence data provider used for building instances
	 * @return a suitable InstanceBuilder
	 * @throws InitializationException no instance builder for the data source
	 */
	public static InstanceBuilder createInstanceBuilderUnigram(String dataSource,
															   OccurrenceDataProvider dataProvider)
			throws InitializationException {

		if(dataSource.toLowerCase().equals(UKWAC)) {
			return new InstanceBuilderUnigramUKWAC(dataProvider);
		}else if(dataSource.toLowerCase().equals(GOOGLE_NGRAM)){
			return new InstanceBuilderUnigramGoogle(dataProvider);
		}else{
			throw new InitializationException("No known occurrence data source found. Please check " +
					"org.dbpedia.spotlight.spot.cooccurrence.datasource in the configuration file.");
			
		}
	}

	
	/**
	 * Creates an instance builder for n-grams (based on the specified data source).
	 *
	 * @param dataSource String id of the data source read from the configuration file
	 * @param dataProvider the occurrence data provider used for building instances
	 * @return a suitable InstanceBuilder
	 * @throws InitializationException no instance builder for the data source
	 */
	public static InstanceBuilder createInstanceBuilderNGram(String dataSource,
															 OccurrenceDataProvider dataProvider)
			throws InitializationException {
		
		if(dataSource.toLowerCase().equals(UKWAC)) {
			return new InstanceBuilderNGramUKWAC(dataProvider);
		}else if(dataSource.toLowerCase().equals(GOOGLE_NGRAM)){
			return new InstanceBuilderNGramGoogle(dataProvider);
		}else{
			throw new InitializationException("No known occurrence data source found. Please check " +
					"org.dbpedia.spotlight.spot.cooccurrence.datasource in the configuration file.");

		}
	
	}
	
}
