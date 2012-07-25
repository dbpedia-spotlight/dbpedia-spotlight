package org.dbpedia.spotlight.spot.cooccurrence.filter;

import org.dbpedia.spotlight.model.SurfaceFormOccurrence;


/**
 * Filter for basic, recurring patterns: dates, times, etc.
 *
 * @author Joachim Daiber
 */

public class FilterPattern extends Filter {

	protected String BLACKLIST_PATTERN = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|day after|" +
			"January( [0-9]{1,2})?|February( [0-9]{1,2})?|March( [0-9]{1,2})?|April( [0-9]{1,2})?|June( [0-9]{1,2})?|July( [0-9]{1,2})?|August( [0-9]{1,2})?|September( [0-9]{1,2})?|October( [0-9]{1,2})?|November( [0-9]{1,2})?|December( [0-9]{1,2})?|" +
			"Jan\\.?( [0-9]{1,2})?|Feb\\.?( [0-9]{1,2})?|Mar\\.?( [0-9]{1,2})?|Apr\\.?( [0-9]{1,2})?|Jun\\.?( [0-9]{1,2})?|Jul\\.?( [0-9]{1,2})?|Aug\\.?( [0-9]{1,2})?|Sept\\.?( [0-9]{1,2})?|Oct\\.?( [0-9]{1,2})?|Nov\\.?( [0-9]{1,2})?|Dec\\.?( [0-9]{1,2})?|" +
			"[Dd]ay[s]?|[Mm]onth[s]?|[Yy]ear[s]?|[Ww]eek[s]?|[Hh]our[s]?|[Mm]inute[s]?|[Ss]econd[s]?|" +
			"[Aa]fternoon[s]?|[Mm]orning[s]?|" +
			"[Yy]ard[s]?|[Mm]ile[s]?|[Mm]meter[s]?|[Kk]kilometer[s]?|" +
			"don|won|can|'s|" +
			"%|per cent|[Pp]ercent|" +
			"Mr\\.?|Mrs\\.?|Ms\\.?|Dr\\.?|Prof\\.?)";


	public boolean applies(SurfaceFormOccurrence surfaceFormOccurrence) {
		return !surfaceFormOccurrence.surfaceForm().name().matches(BLACKLIST_PATTERN);
	}


	public String name() {
		return "Filters surface forms by simple, Gazetteer-like patterns.";
	}
	
}
