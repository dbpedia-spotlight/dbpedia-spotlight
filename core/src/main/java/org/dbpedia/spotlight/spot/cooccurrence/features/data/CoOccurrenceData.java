package org.dbpedia.spotlight.spot.cooccurrence.features.data;


/**
 * Co-occurrence data for a surface form and words in its context.
 *
 * @author Joachim Daiber
 */

public class CoOccurrenceData {

	/**
	 * Count of the unit in Web and Corpus.
	 */
	private long unitCountCorpus = 0;
	private long unitCountWeb = 0;


	/**
	 * Significance of the unit in Web and Corpus.
	 */
	private float unitSignificanceCorpus = 0;
	private float unitSignificanceWeb = 0;


	/**
	 * Get the frequency of the co-occurrence in the Wikipedia corpus.
	 * @return frequency in Wikipedia corpus
	 */
	public long getUnitCountCorpus() {
		return unitCountCorpus;
	}
	

	/**
	 * Get the frequency of the co-occurrence in the Web corpus.
	 * @return frequency in Web
	 */
	public long getUnitCountWeb() {
		return unitCountWeb;
	}

	
	/**
	 * Get the significance for the co-occurrence in the Wikipedia corpus.
	 * @return significance
	 */
	public float getUnitSignificanceCorpus() {
		return unitSignificanceCorpus;
	}

	
	/**
	 * Get the significance for the co-occurrence in the Web corpus.
	 * @return significance
	 */
	public float getUnitSignificanceWeb() {
		return unitSignificanceWeb;
	}

	
	/**
	 * Create Co-occurrence data with frequency and significance.
	 *
	 * @param unitCountWikipedia frequency in Wikipedia
	 * @param unitCountWeb frequency in Web corpus
	 * @param unitSignificanceWikipedia significance in Wikipedia
	 * @param unitSignificanceWeb significance in Web
	 */
	public CoOccurrenceData(long unitCountWikipedia, long unitCountWeb, float unitSignificanceWikipedia, float unitSignificanceWeb) {
		this.unitCountCorpus = unitCountWikipedia;
		this.unitCountWeb = unitCountWeb;
		this.unitSignificanceCorpus = unitSignificanceWikipedia;
		this.unitSignificanceWeb = unitSignificanceWeb;
	}

	@Override
	public String toString() {
		return "CoOccurrenceData[" +
				"  unitCountCorpus=" + unitCountCorpus +
				", unitCountWeb=" + unitCountWeb +
				", unitSignificanceCorpus=" + unitSignificanceCorpus +
				", unitSignificanceWeb=" + unitSignificanceWeb +
				']';
	}
	
}
