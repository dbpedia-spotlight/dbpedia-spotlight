package org.dbpedia.spotlight.spot.cooccurrence.features.data;

/**
 * Occurrence data for a surface form.
 * 
 * @author Joachim Daiber
 */

public class CandidateData {

	
	/**
	 * Create occurrence data for a candidate.
	 * 
	 * @param id id of the candidate in the database
	 * @param token String representation of the candidate
	 * @param countWikipedia frequency in the Wikipedia corpus
	 * @param countWeb count frequency in the Web corpus
	 */
	public CandidateData(long id, String token, long countWikipedia, long countWeb) {
		this.id = id;
		this.token = token;
		this.countCorpus = countWikipedia;
		this.countWeb = countWeb;
	}


	/**
	 * ID and token of the candidate.
	 */
	private long id;
	private String token;

	/**
	 * Counts for the single word in Corpus and Web.
	 */
	private Long countCorpus;
	private Long countWeb;


	/**
	 * Get the database id of the candidate.
	 * 
	 * @return database id
	 */
	public long getId() {
		return id;
	}


	/**
	 * Get the String representation of the candidate.
	 *
	 * @return candidate String
	 */
	public String getToken() {
		return token;
	}


	/**
	 * Get the frequency of the candidate in Wikipedia.
	 * @return frequency in Wikipedia corpus
	 */
	public Long getCountWikipedia() {
		return countCorpus;
	}


	/**
	 * Get the frequency of the candidate in the Web corpus.
	 * @return frequency in Web corpus
	 */
	public Long getCountWeb() {
		return countWeb;
	}

	@Override
	public String toString() {
		return "CandidateData[" +
				"id=" + id +
				", token='" + token + '\'' +
				", countCorpus=" + countCorpus +
				", countWeb=" + countWeb +
				']';
	}
}
