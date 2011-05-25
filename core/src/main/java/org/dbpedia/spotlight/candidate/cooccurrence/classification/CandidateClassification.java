package org.dbpedia.spotlight.candidate.cooccurrence.classification;

/**
 * Simple candiate classification
 *
 * @author Joachim Daiber
 */

public class CandidateClassification {


	private double confidence;
	private CandidateClass candidateClass;

	public double getConfidence() {
		return confidence;
	}

	public CandidateClass getCandidateClass() {
		return candidateClass;
	}

	public CandidateClassification(double confidence, CandidateClass candidateClass) {
		this.confidence = confidence;
		this.candidateClass = candidateClass;
	}


}