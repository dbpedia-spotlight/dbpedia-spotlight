package org.dbpedia.spotlight.candidate.cooccurrence.features.training;

import org.dbpedia.spotlight.candidate.cooccurrence.CandidateUtil;
import org.dbpedia.spotlight.candidate.cooccurrence.features.AttributesUnigram;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProviderSQL;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterPOS;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterPattern;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterTermsize;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.json.JSONException;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Generates training data for a unigram classifier.
 *
 * {@see AnnotatedDatasetEnricher}
 *
 * @author Joachim Daiber
 */
public class AnnotatedDatasetEnricherUnigram extends AnnotatedDatasetEnricher {

	public AnnotatedDatasetEnricherUnigram(SpotlightConfiguration configuration) throws ConfigurationException, IOException {
		super();

		filters.add(new FilterTermsize(FilterTermsize.Termsize.unigram));
		filters.add(new FilterPOS());
		filters.add(new FilterPattern());

		header = new Instances("UnigramTraining", buildAttributeList(), buildAttributeList().size());

		dataProvider  = OccurrenceDataProviderSQL.getInstance(configuration);

	}

	@Override
	public Instance buildInstance(OccurrenceInstance trainingInstance, OccurrenceDataProvider dataProvider) {
		DenseInstance instance = new DenseInstance(buildAttributeList().size());
		instance.setDataset(header);
		return CandidateUtil.buildInstanceUnigram(trainingInstance, dataProvider, instance, true);
	}

	@Override
	protected ArrayList<Attribute> buildAttributeList() {
		return AttributesUnigram.attributeList;
	}

	public static void main(String[] args) throws ConfigurationException, IOException, JSONException {

		AnnotatedDatasetEnricherUnigram annotatedDatasetEnricherUnigram = new AnnotatedDatasetEnricherUnigram(new SpotlightConfiguration("conf/server.properties"));

		OccurrenceDataset trainingData = new OccurrenceDataset(new File("/Users/jodaiber/Documents/workspace/ba/BachelorThesis/01 Evaluation/02 Annotation/Software/custom/src/annotation/test.json"), OccurrenceDataset.Format.JSON);
		annotatedDatasetEnricherUnigram.writeDataset(trainingData, new File("/Users/jodaiber/Desktop/UnigramTraining.xrff"));

	}

	
}
