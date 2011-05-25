package org.dbpedia.spotlight.candidate.cooccurrence.filter;

import org.apache.commons.collections.Predicate;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeUtil;


/**
 * A filter for candidate term size (unigram, bigram, trigram, ...)
 *
 * @author Joachim Daiber
 */

public class FilterTermsize extends Filter {

	public static enum Termsize {none, unigram, bigram, trigram, n_4, n_5, n_x}
	protected Termsize filteredSize;

	public FilterTermsize(Termsize filteredSize) {
		this.filteredSize = filteredSize;
	}

	@Override
	protected Predicate getPredicate() {

		return new Predicate() {

			@Override
			public boolean evaluate(Object object) {
				if(object.getClass().equals(String.class)) {

					if(!inverse)
						return applies((String) object);
					else
						return !applies((String) object);
				}else{
					if(!inverse)
						return applies((SurfaceFormOccurrence) object);
					else
						return !applies((SurfaceFormOccurrence) object);
				}
			}

		};
		
	}
	
	@Override
	public boolean applies(SurfaceFormOccurrence surfaceFormOccurrence) {
		return getTermSize(surfaceFormOccurrence.surfaceForm()) == filteredSize;
	}

	public boolean applies(String surfaceForm) {
		return getTermSize(surfaceForm) == filteredSize;
	}
	

	/**
	* Returns the size of a candidate surface form.
	*
	* @param surfaceForm
	* @return
	*/

   public static Termsize getTermSize(String surfaceForm) {

	   switch (LingPipeUtil.getTokens(surfaceForm).size()) {
		   case 0:
			   return Termsize.none;
		   case 1:
			   return Termsize.unigram;
		   case 2:
			   return Termsize.bigram;
		   case 3:
			   return Termsize.trigram;
		   case 4:
			   return Termsize.n_4;
		   case 5:
			   return Termsize.n_5;
		   default:
			   return Termsize.n_x;
	   }

   }

	public static Termsize getTermSize(SurfaceForm surfaceForm) {
		return getTermSize(surfaceForm.name());
	}

	@Override
	public String name() {
		return "Filter for the number of tokens in a term candidate.";
	}





}
