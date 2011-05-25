package org.dbpedia.spotlight.candidate.cooccurrence.features.data;

/**
 * Occurrence data for a term candidate.
 * 
 * @author Joachim Daiber
 */

public class CandidateData {

	public CandidateData(long id, String token, long countCorpus, long countWeb) {
		this.id = id;
		this.token = token;
		this.countCorpus = countCorpus;
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
	

	public long getId() {
		return id;
	}

	public String getToken() {
		return token;
	}

	public Long getCountCorpus() {
		return countCorpus;
	}

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
