package org.dbpedia.spotlight.candidate.cooccurrence.training;

import org.dbpedia.spotlight.candidate.cooccurrence.InstanceBuilderFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProviderSQL;
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
 * Generates training data for an ngram classifier.
 *
 * {@see AnnotatedDatasetEnricher}
 *
 * @author Joachim Daiber
 */
public class AnnotatedDatasetEnricherNGram extends AnnotatedDatasetEnricher {

	public AnnotatedDatasetEnricherNGram(SpotlightConfiguration configuration) throws ConfigurationException, IOException, InitializationException {
		super(configuration);

		OccurrenceDataProviderSQL.initialize(configuration.getSpotterConfiguration());
		dataProvider  = OccurrenceDataProviderSQL.getInstance();

		instanceBuilder = InstanceBuilderFactory.createInstanceBuilderNGram(
				configuration.getSpotterConfiguration().getCandidateOccurrenceDataSource(), dataProvider);
		instanceBuilder.setVerboseMode(true);

		/** Filter the data set: */
		FilterTermsize filterTermsize = new FilterTermsize(FilterTermsize.Termsize.unigram, luceneFactory.textUtil());
		filterTermsize.inverse();

		FilterPattern filterPattern = new FilterPattern();

		filters.add(filterTermsize);
		filters.add(filterPattern);

		/** Create a new header object: */
		header = new Instances("NgramTraining", buildAttributeList(), buildAttributeList().size());

	}

	public static void main(String[] args) throws ConfigurationException, IOException, JSONException, InitializationException {

		AnnotatedDatasetEnricherNGram annotatedDatasetEnricherNGram = new AnnotatedDatasetEnricherNGram(new SpotlightConfiguration("conf/server.properties"));

		OccurrenceDataset trainingData = new OccurrenceDataset(new File("/Users/jodaiber/Documents/workspace/ba/BachelorThesis/01 Evaluation/02 Annotation/Software/custom/src/annotation/second_run.json"), OccurrenceDataset.Format.JSON);
		annotatedDatasetEnricherNGram.writeDataset(trainingData, new File("/Users/jodaiber/Desktop/NgramTraining.xrff"));

	}



}
