package org.dbpedia.spotlight.candidate.cooccurrence.classification;

import org.dbpedia.spotlight.candidate.cooccurrence.CandidateUtil;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import weka.core.DenseInstance;
import weka.core.Instance;

/**
 * Candidate classifier for n-gram candidates.
 *
 * @author Joachim Daiber
 */

public class CandidateClassifierNGram extends CandidateClassifier {

	public CandidateClassifierNGram(String modelFile, OccurrenceDataProvider dataProvider) throws Exception {
		super(modelFile, dataProvider);
	}

	@Override
	protected Instance buildInstance(SurfaceFormOccurrence surfaceFormOccurrence) {
		Instance instance = new DenseInstance(header.numAttributes());
		instance.setDataset(header);

		return CandidateUtil.buildInstanceNGram(surfaceFormOccurrence, dataProvider, instance, verboseMode);
	}
	
}
