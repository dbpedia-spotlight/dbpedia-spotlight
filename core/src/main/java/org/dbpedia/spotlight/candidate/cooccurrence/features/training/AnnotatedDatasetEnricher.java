package org.dbpedia.spotlight.candidate.cooccurrence.features.training;


import com.aliasi.sentences.IndoEuropeanSentenceModel;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.Filter;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeFactory;
import org.json.JSONException;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.XRFFSaver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Reads an annotated dataset that contains surface form occurrences and
 * manual classifcation by a human annotator and produces an XRFF file
 * for training a classifier.
 *
 * @author Joachim Daiber
 */

public abstract class AnnotatedDatasetEnricher {

	protected List<Filter> filters = new LinkedList<Filter>();
	protected OccurrenceDataProvider dataProvider;
	protected Instances header;


	protected AnnotatedDatasetEnricher() throws IOException, ConfigurationException {

		SpotlightConfiguration spotlightConfiguration = new SpotlightConfiguration("conf/server.properties");

		LingPipeFactory.setTaggerModelFile(new File(spotlightConfiguration.getTaggerFile()));
		LingPipeFactory.setSentenceModel(new IndoEuropeanSentenceModel());

				
	}

	public void writeDataset(OccurrenceDataset trainingData, File targetFile) throws IOException, ConfigurationException, JSONException {

		/*
		 * From a Java best practices point of view, this should be
		 * a List. However, the WEKA constructor explicitly requires
		 * an ArrayList.
		 */
		ArrayList<Attribute> attributeList = buildAttributeList();

		trainingData.filter(filters);

		Instances instances = new Instances("Training", attributeList, trainingData.size());
		int i = 1;

		for(OccurrenceInstance trainingInstance : trainingData.getInstances()) {

			/** Set the annotation */
			int annotationValue = -1;

			switch (trainingInstance.getCandidateClass()) {

				case term:
					annotationValue = 0;
					break;

				case common:
					annotationValue = 1;
					break;

				case part:
					annotationValue = 1;
					break;

			}

			System.err.println(i + ": " + trainingInstance.getCandidateClass() + " #############################################");

			Instance instance = null;
			instance = buildInstance(trainingInstance, this.dataProvider);

			instance.setValue(attributeList.size() - 1, annotationValue);

			/** Add the instance */
			if(annotationValue >= 0)
				instances.add(instance);

			i++;
		}

		XRFFSaver xrffSaver = new XRFFSaver();
		xrffSaver.setInstances(instances);

		try {
			xrffSaver.setFile(targetFile);
			xrffSaver.setDestination(targetFile);
		} catch (IOException e) {
			System.err.println("Could not write UnigramTraining file.");
		}

		xrffSaver.writeBatch();
	}

	public abstract Instance buildInstance(OccurrenceInstance trainingInstance, OccurrenceDataProvider dataProvider);
	protected abstract ArrayList<Attribute> buildAttributeList();

}
