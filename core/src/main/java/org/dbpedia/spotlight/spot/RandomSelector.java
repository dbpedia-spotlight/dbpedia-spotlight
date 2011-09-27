package org.dbpedia.spotlight.spot;

import org.dbpedia.spotlight.model.SurfaceFormOccurrence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Random {@link SpotSelector} for evaluation.
 *
 * @author Joachim Daiber
 */

public class RandomSelector implements UntaggedSpotSelector {

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
