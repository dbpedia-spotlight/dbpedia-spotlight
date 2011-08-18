package org.dbpedia.spotlight.candidate.cooccurrence.classification;

/**
 * Candidate classification with confidence value.
 *
 * A candidate is assigned to a classification in {@link CandidateClass} with a confidence value between
 * 0 and 1.
 *
 * @author Joachim Daiber
 */

public class CandidateClassification {

	private double confidence;
	private CandidateClass candidateClass;


	/**
	 * Get the confidence value for the classification.
	 *
	 * @return confidence between 0 and 1
	 */
	public double getConfidence() {
		return confidence;
	}


	/**
	 * Get the class the candidate was assigned to.
	 * @return assigned candidate class
	 */
	public CandidateClass getCandidateClass() {
		return candidateClass;
	}


	/**
	 * Creates a classification with a confidence value between 0 and 1.
	 * 
	 * @param confidence confidence of the classification
	 * @param candidateClass assigned candidate class
	 */
	public CandidateClassification(double confidence, CandidateClass candidateClass) {
		this.confidence = confidence;
		this.candidateClass = candidateClass;
	}


}