package org.dbpedia.spotlight.spot.cooccurrence.features.data;


import org.dbpedia.spotlight.exceptions.ItemNotFoundException;

import java.util.List;

/**
 * Provides occurrence data for tokens.
 */

public interface OccurrenceDataProvider {

	/**
	 * Get occurrence and query data for a candidate.
	 *
	 * @param candidate String representation of a term candidate.
	 * @return information about the occurrence of the term candidate
	 * @throws ItemNotFoundException no information about the term candidate is available
	 */
	public CandidateData getCandidateData(String candidate) throws ItemNotFoundException;


	/**
	 * Get occurrence data for a bigram.
	 *
	 * @param word1 CandidateData of the first word.
	 * @param word2 CandidateData of the second word.
	 * @return information about the occurrence of the bigram
	 * @throws ItemNotFoundException no information about the bigram is available
	 */
	public CoOccurrenceData getBigramData(CandidateData word1, CandidateData word2)
			throws ItemNotFoundException;


	/**
	 * Get occurrence data for a trigram.
	 *
	 * @param word1 CandidateData of the first word.
	 * @param word2 CandidateData of the second word.
	 * @param word3 CandidateData of the third word.
	 * @return information about the occurrence of the trigram
	 * @throws ItemNotFoundException no information about the trigram is available
	 */
	public CoOccurrenceData getTrigramData(CandidateData word1, CandidateData word2, CandidateData word3)
			throws ItemNotFoundException;


	/**
	 * Get information about the co-occurrence of the candidate with any token in the sentence.
	 *
	 * @param candidate CandidateData of the first word.
	 * @param tokens a list of possibly co-occurring tokens.
	 * @return information about the co-occurrence of the candidate and the tokens in the List.
	 */
	public List<CoOccurrenceData> getSentenceData(CandidateData candidate, List<String> tokens);


}
