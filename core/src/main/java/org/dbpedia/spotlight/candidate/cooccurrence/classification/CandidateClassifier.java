package org.dbpedia.spotlight.candidate.cooccurrence.classification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProviderSQL;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;


/**
 * Base class for candidate classifiers.
 *
 * @author Joachim Daiber
 */

public abstract class CandidateClassifier {

	Log LOG = LogFactory.getLog(this.getClass());

	protected Instances header;
	protected Classifier classifier;
	protected String modelFile;
	protected OccurrenceDataProvider dataProvider;
	protected final double MIN_CONFIDENCE = 0.55;

	protected boolean verboseMode = false;


	public CandidateClassifier(String modelFile) throws Exception {
		this.modelFile = modelFile;
		this.dataProvider = OccurrenceDataProviderSQL.getInstance();

		initialize();
	}

	

	protected void initialize() throws Exception {
		Object o[] = SerializationHelper.readAll(modelFile);
		classifier = (Classifier) o[0];
		header = (Instances) o[1];
		header.setClassIndex(header.numAttributes() - 1);
		LOG.trace("Successfully deserialzed Classifier " + classifier);

	}

	public CandidateClassification classify(SurfaceFormOccurrence surfaceFormOccurrence) {
		Instance instance = buildInstance(surfaceFormOccurrence);

		try {
			double candidateClassification = classifier.classifyInstance(instance);


			double[] distributionForInstance = classifier.distributionForInstance(instance);
			double confidence = distributionForInstance[(int) candidateClassification];
			CandidateClass candidateClass = candidateClassification == 0 && confidence > MIN_CONFIDENCE ? CandidateClass.term : CandidateClass.common;

			return new CandidateClassification(confidence, candidateClass);
		} catch (Exception e) {
			LOG.error("Exception while classifing " + e);
			return null;
		}

	}

	protected abstract Instance buildInstance(SurfaceFormOccurrence surfaceFormOccurrence);

	/**
	 * In verbose mode, the classifier explains why and how it made its classification decision.
	 * 
	 * @param verboseMode put classifier in verbose mode?
	 */
	public void setVerboseMode(boolean verboseMode) {
		this.verboseMode = verboseMode;
	}
}
