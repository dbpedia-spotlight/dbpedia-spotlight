package org.dbpedia.spotlight.spot.cooccurrence.training;

import org.dbpedia.spotlight.spot.cooccurrence.InstanceBuilderFactory;
import org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProviderSQL;
import org.dbpedia.spotlight.spot.cooccurrence.filter.FilterPattern;
import org.dbpedia.spotlight.spot.cooccurrence.filter.FilterTermsize;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SpotlightFactory;
import org.json.JSONException;
import weka.core.Instances;

import java.io.File;
import java.io.IOException;

/**
 * Generates training data for an ngram classifier.
 *
 * {@see AnnotatedDatasetEnricher}
 *
 * @author Joachim Daiber
 */
public class AnnotatedDatasetEnricherNGram extends AnnotatedDatasetEnricher {


	/**
	 * Create a new AnnotatedDatasetEnricher for ngrams.
	 *
	 * @param configuration Spotlight configuration object
	 * @throws ConfigurationException Error in configuration.
	 * @throws IOException Error when reading file.
	 * @throws InitializationException Error in Initialization.
	 */
	public AnnotatedDatasetEnricherNGram(SpotlightConfiguration configuration) throws ConfigurationException, IOException, InitializationException {
		super(configuration);

		OccurrenceDataProviderSQL.initialize(configuration.getSpotterConfiguration());
		dataProvider  = OccurrenceDataProviderSQL.getInstance();

		instanceBuilder = InstanceBuilderFactory.createInstanceBuilderNGram(
				configuration.getSpotterConfiguration().getCoOcSelectorDatasource(), dataProvider);
		instanceBuilder.setVerboseMode(true);

		/** Filter the data set: */
		FilterTermsize filterTermsize = new FilterTermsize(FilterTermsize.Termsize.unigram, spotlightFactory.textUtil());
		filterTermsize.inverse();

		FilterPattern filterPattern = new FilterPattern();

		filters.add(filterTermsize);
		filters.add(filterPattern);

		/** Create a new header object: */
		header = new Instances("NgramTraining", buildAttributeList(), buildAttributeList().size());

	}

	
	public static void main(String[] args) throws ConfigurationException, IOException, JSONException, InitializationException {

		SpotlightConfiguration configuration = new SpotlightConfiguration("conf/server.properties");


		AnnotatedDatasetEnricherNGram annotatedDatasetEnricherNGram = new AnnotatedDatasetEnricherNGram(configuration);
		SpotlightFactory spotlightFactory = new SpotlightFactory(configuration);
		
		AnnotatedDataset trainingData = new AnnotatedDataset(
				new File("/Users/jodaiber/Documents/workspace/ba/BachelorThesis/01 Evaluation/02 Annotation/Software/custom/src/annotation/final.train.json"),
				AnnotatedDataset.Format.JSON, spotlightFactory);
		annotatedDatasetEnricherNGram.writeDatasetXRFF(trainingData, new File("/Users/jodaiber/Desktop/final.ngram.xrff"));

	}



}
