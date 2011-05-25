package org.dbpedia.spotlight.candidate.cooccurrence.classification;

import org.dbpedia.spotlight.candidate.cooccurrence.CandidateUtil;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import weka.core.DenseInstance;
import weka.core.Instance;

/**
 * Candidate classifier for unigram candidates.
 *
 * @author Joachim Daiber
 */
public class CandidateClassifierUnigram extends CandidateClassifier {

	public CandidateClassifierUnigram(String modelFile) throws Exception {
		super(modelFile);
	}

	@Override
	protected Instance buildInstance(SurfaceFormOccurrence surfaceFormOccurrence) {
		Instance instance = new DenseInstance(header.numAttributes());
		instance.setDataset(header);
		
		return CandidateUtil.buildInstanceUnigram(surfaceFormOccurrence, dataProvider, instance, verboseMode);
	}
	
}
