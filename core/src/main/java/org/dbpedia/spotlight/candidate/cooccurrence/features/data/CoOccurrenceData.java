package org.dbpedia.spotlight.candidate.cooccurrence.features.data;


/**
 * Co-Occurrence data for multiple tokens.
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
	

	public long getUnitCountCorpus() {
		return unitCountCorpus;
	}

	public long getUnitCountWeb() {
		return unitCountWeb;
	}

	public float getUnitSignificanceCorpus() {
		return unitSignificanceCorpus;
	}

	public float getUnitSignificanceWeb() {
		return unitSignificanceWeb;
	}

	public CoOccurrenceData(long unitCountCorpus, long unitCountWeb, float unitSignificanceCorpus, float unitSignificanceWeb) {
		this.unitCountCorpus = unitCountCorpus;
		this.unitCountWeb = unitCountWeb;
		this.unitSignificanceCorpus = unitSignificanceCorpus;
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
