package org.dbpedia.spotlight.spot.cooccurrence.training;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.spot.cooccurrence.weka.InstanceBuilder;
import org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.spot.cooccurrence.filter.Filter;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.model.SpotlightFactory;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.json.JSONException;
import weka.core.Attribute;
import weka.core.DenseInstance;
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
 * manual classification by a human annotator and produces an XRFF file
 * for training a classifier.
 *
 * @author Joachim Daiber
 */

public abstract class AnnotatedDatasetEnricher {

	Log LOG = LogFactory.getLog(this.getClass());

	protected InstanceBuilder instanceBuilder;
	protected List<Filter> filters = new LinkedList<Filter>();
	protected OccurrenceDataProvider dataProvider;
	protected Instances header;
	protected SpotlightFactory spotlightFactory;

	/**
	 * Constuctor for subclasses.
	 *
	 * @param configuration SpotlightConfiguration to create a SpotlightFactory from
	 * @throws IOException Error in reading annotation files.
	 * @throws ConfigurationException Error in Configuration.
	 */
	protected AnnotatedDatasetEnricher(SpotlightConfiguration configuration) throws IOException, ConfigurationException {
		spotlightFactory = new SpotlightFactory(configuration);
	}


	/**
	 * Write the generated data set to the specified files.
	 *
	 * @param trainingData Generated training data set
	 * @param targetFile File, the data set should be written to
	 * @throws IOException Could not write file
	 * @throws ConfigurationException There was a problem with loading from the Configuration
	 * @throws JSONException Could not parse JSON-serialized annotation sheet.
	 */
	public void writeDatasetXRFF(AnnotatedDataset trainingData, File targetFile) throws IOException, ConfigurationException, JSONException {

		/*
		 * From a Java best practices point of view, this should be
		 * a List. However, the WEKA constructor explicitly requires
		 * an ArrayList.
		 */
		ArrayList<Attribute> attributeList = buildAttributeList();

		trainingData.filter(filters);

		Instances instances = new Instances("Training", attributeList, trainingData.size());
		int i = 1;

		for(AnnotatedSurfaceFormOccurrence trainingInstance : trainingData.getInstances()) {

			/** Set the annotation */
			int annotationValue = -1;

			switch (trainingInstance.getSpotClass()) {

				case valid:
					annotationValue = 0;
					break;

				case common:
					annotationValue = 1;
					break;

				case part:
					annotationValue = 1;
					break;

			}

			System.err.println(i + ": " + trainingInstance.getSpotClass() + " #############################################");
			Instance instance = buildInstance(trainingInstance);
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

	
	/**
	 * Build the instance with the suitable InstanceBuilder.
	 *
	 * @param trainingInstance surface form occurrence of the candidate
	 * @return WEKA Instance for the surface form occurrence
	 */
	public Instance buildInstance(AnnotatedSurfaceFormOccurrence trainingInstance) {
		DenseInstance instance = new DenseInstance(buildAttributeList().size());
		instance.setDataset(header);
		return instanceBuilder.buildInstance(trainingInstance, instance);
	}

	
	/**
	 * Build the List of attributes. This must be an {@link ArrayList}, because of the method
	 * signature in WEKA.
	 *
	 * @return List of Attributes
	 */
	protected ArrayList<Attribute> buildAttributeList() {
		return instanceBuilder.buildAttributeList();
	}
	
}
