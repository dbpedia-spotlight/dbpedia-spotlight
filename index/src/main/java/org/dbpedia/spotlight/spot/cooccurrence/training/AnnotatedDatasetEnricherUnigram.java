package org.dbpedia.spotlight.spot.cooccurrence.training;

import org.dbpedia.spotlight.spot.cooccurrence.InstanceBuilderFactory;
import org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProviderSQL;
import org.dbpedia.spotlight.spot.cooccurrence.filter.FilterPOS;
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
 * Generates training data for a unigram classifier.
 *
 * {@see AnnotatedDatasetEnricher}
 *
 * @author Joachim Daiber
 */
public class  AnnotatedDatasetEnricherUnigram extends AnnotatedDatasetEnricher {

	/**
	 * Create a new AnnotatedDatasetEnricher for unigrams.
	 *
	 * @param configuration Spotlight configuration object
	 * @throws ConfigurationException Error in configuration.
	 * @throws IOException Error when reading file.
	 * @throws InitializationException Error in Initialization.
	 */
	public AnnotatedDatasetEnricherUnigram(SpotlightConfiguration configuration) throws ConfigurationException,
			IOException, InitializationException {
		
		super(configuration);

		LOG.info("Loading occurrence data...");
		OccurrenceDataProviderSQL.initialize(configuration.getSpotterConfiguration());
		LOG.info("Done.");
		dataProvider  = OccurrenceDataProviderSQL.getInstance();

		instanceBuilder = InstanceBuilderFactory.createInstanceBuilderUnigram(
				configuration.getSpotterConfiguration().getCoOcSelectorDatasource(),
				dataProvider);
		
		instanceBuilder.setVerboseMode(true);

		filters.add(new FilterTermsize(FilterTermsize.Termsize.unigram));
		filters.add(new FilterPOS());
		filters.add(new FilterPattern());

		header = new Instances("UnigramTraining", buildAttributeList(), buildAttributeList().size());

	}

	public static void main(String[] args) throws ConfigurationException, IOException, JSONException, InitializationException {

		SpotlightConfiguration configuration = new SpotlightConfiguration("conf/server.properties");
		SpotlightFactory spotlightFactory = new SpotlightFactory(configuration);

		AnnotatedDatasetEnricherUnigram annotatedDatasetEnricherUnigram = new AnnotatedDatasetEnricherUnigram(configuration);
		AnnotatedDataset trainingData = new AnnotatedDataset(
				new File("/Users/jodaiber/Documents/workspace/ba/BachelorThesis/01 Evaluation/02 Annotation/Software/custom/src/annotation/final.train.json"),
				AnnotatedDataset.Format.JSON, spotlightFactory);
		
		annotatedDatasetEnricherUnigram.writeDatasetXRFF(trainingData, new File("/Users/jodaiber/Desktop/final.unigram.xrff"));
		
	}

	
}
