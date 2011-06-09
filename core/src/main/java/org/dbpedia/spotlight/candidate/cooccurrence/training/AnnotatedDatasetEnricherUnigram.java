package org.dbpedia.spotlight.candidate.cooccurrence.training;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.InstanceBuilderFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProviderSQL;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterPOS;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterPattern;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterTermsize;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
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
public class AnnotatedDatasetEnricherUnigram extends AnnotatedDatasetEnricher {

	Log LOG = LogFactory.getLog(this.getClass());

	public AnnotatedDatasetEnricherUnigram(SpotlightConfiguration configuration) throws ConfigurationException, IOException, InitializationException {
		super(configuration);

		LOG.info("Loading occurrence data...");
		OccurrenceDataProviderSQL.initialize(configuration.getSpotterConfiguration());
		LOG.info("Done.");
		dataProvider  = OccurrenceDataProviderSQL.getInstance();

		instanceBuilder = InstanceBuilderFactory.createInstanceBuilderUnigram(
				configuration.getSpotterConfiguration().getCandidateOccurrenceDataSource(),
				dataProvider);
		
		instanceBuilder.setVerboseMode(true);

		filters.add(new FilterTermsize(FilterTermsize.Termsize.unigram));
		filters.add(new FilterPOS());
		filters.add(new FilterPattern());

		header = new Instances("UnigramTraining", buildAttributeList(), buildAttributeList().size());

	}

	public static void main(String[] args) throws ConfigurationException, IOException, JSONException, InitializationException {

		AnnotatedDatasetEnricherUnigram annotatedDatasetEnricherUnigram = new AnnotatedDatasetEnricherUnigram(new SpotlightConfiguration("conf/server.properties"));

		OccurrenceDataset trainingData = new OccurrenceDataset(new File("/Users/jodaiber/Documents/workspace/ba/BachelorThesis/01 Evaluation/02 Annotation/Software/custom/src/annotation/test.json"), OccurrenceDataset.Format.JSON);
		annotatedDatasetEnricherUnigram.writeDataset(trainingData, new File("/Users/jodaiber/Desktop/UnigramTraining.xrff"));
		
	}

	
}
