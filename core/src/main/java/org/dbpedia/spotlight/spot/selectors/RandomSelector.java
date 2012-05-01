package org.dbpedia.spotlight.spot.selectors;

import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.spot.SpotSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Random {@link org.dbpedia.spotlight.spot.SpotSelector} for evaluation.
 *
 * @author Joachim Daiber
 */

public class RandomSelector implements SpotSelector {

	int valid;
	int common;

	/**
	 * Randomly selects spots while trying to match the distribution of valid
	 * and common occurrences in the corpus.
	 *
	 * @param valid number of valid occurrences
	 * @param common number of common occurrences
	 */
	public RandomSelector(int valid, int common) {
		this.common = common;
		this.valid = valid;
	}
	

	public List<SurfaceFormOccurrence> select(List<SurfaceFormOccurrence> occs) {
		List<SurfaceFormOccurrence> occurrences = new ArrayList<SurfaceFormOccurrence>(occs);
		Collections.shuffle(occurrences);
		return occurrences.subList(0, (int) (((valid / ((float) (common+valid)))) * occs.size()));
	}



}
