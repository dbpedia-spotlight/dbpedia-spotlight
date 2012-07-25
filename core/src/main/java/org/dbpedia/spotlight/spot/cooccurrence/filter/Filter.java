package org.dbpedia.spotlight.spot.cooccurrence.filter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;

import java.util.Collection;

/**
 * Base class for filters for surface form occurrences
 * for spot selection.
 * 
 * @author Joachim Daiber
 */

public abstract class Filter {

	/**
	 * Boolean stating if the filter is inversed.
	 * Example: If the filter is set to only keep unigram terms, it will keep all but unigram
	 * terms if it is inversed.
	 */
	protected boolean inverse = false;


	/**
	 * The Predicate is used to filter a list of surface form occurrences.
	 * 
	 * @return Predicate to filter surface form occurrences.
	 */
	protected Predicate getPredicate() {
		return new Predicate() {

		@Override
		public boolean evaluate(Object object) {

			if(!inverse)
				return applies((SurfaceFormOccurrence) object);
			else
				return !applies((SurfaceFormOccurrence) object);
		}
		};
		
	}


	/**
	 * Does the filter apply to a given surface form occurrence?
	 *
	 * @param surfaceFormOccurrence surface form occurrence to to be checked
	 * @return true if filter applies
	 */
	public abstract boolean applies(SurfaceFormOccurrence surfaceFormOccurrence);


	/**
	 * Filters a List in-place.
	 *
	 * @param surfaceFormOccurrences list of surface form occurrences to be filtered
	 */
	public void apply(Collection<?> surfaceFormOccurrences) {
		CollectionUtils.filter(surfaceFormOccurrences, getPredicate());
	}


	/**
	 * Every filter has a name.
	 * @return name of the filter
	 */
	public abstract String name();


	/**
	 * Inverse the filter. Example: If the filter is set to keep only unigram terms, it will keep all
	 * but unigram terms if inversed.
	 */
	public void inverse() {
		this.inverse = true;
	}

}