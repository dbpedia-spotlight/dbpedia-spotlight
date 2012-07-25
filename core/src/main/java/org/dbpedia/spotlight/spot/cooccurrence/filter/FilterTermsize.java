package org.dbpedia.spotlight.spot.cooccurrence.filter;

import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.tagging.TextUtil;


/**
 * Filter for candidate term size (unigram, bigram, trigram, ...)
 *
 * @author Joachim Daiber
 */

public class FilterTermsize extends Filter {

	private final Log LOG = LogFactory.getLog(this.getClass());

	private TextUtil textUtil;
	public static enum Termsize {none, unigram, bigram, trigram, n_4, n_5, n_x}
	protected Termsize filteredSize;


	/**
	 * Create a filter for term size with textUtil provided as textUtil.
	 *
	 * @param filteredSize size of the surface forms to filter for
	 * @param textUtil TextUtil for tokenizing surface forms
	 */

	public FilterTermsize(Termsize filteredSize, TextUtil textUtil) {
		this.filteredSize = filteredSize;
		this.textUtil = textUtil;
	}

	/**
	 * Create a filter for term size which assumes that the surface forms to be
	 * filtered contain {@link org.dbpedia.spotlight.model.TaggedText} object.
	 *
	 * @param filteredSize
	 */
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


		if(textUtil != null) {
			return applies(surfaceFormOccurrence.surfaceForm().name());
		}else if(surfaceFormOccurrence.context() instanceof TaggedText){
			int iTokens = ((TaggedText) surfaceFormOccurrence.context())
					.taggedTokenProvider().getTaggedTokens(surfaceFormOccurrence).size();
			return getTermSize(iTokens) == filteredSize;
		}else{
			/**
			 * No TextUtil has been specified and the surface forms contain no TaggedText.
			 */

			LOG.error("Could not count number of tokens in surface form since the surface form does not " +
					"contain tagged text and no textUtil was provided.");

			return true;
		}

	}

	private boolean applies(String surfaceForm) {

		if(textUtil != null) {
			return getTermSize(surfaceForm, textUtil) == filteredSize;
		}else {
			/**
			 * No TextUtil has been specified and the surface forms contain no TaggedText.
			 */

			LOG.error("Could not count number of tokens in surface form since the surface form does not " +
					"contain tagged text and no textUtil was provided.");

			return true;
		}

	}



	/**
	* Returns the size of a candidate surface form.
	*
	* @param surfaceForm
	* @param textUtil
	 * @return
	*/

   public static Termsize getTermSize(String surfaceForm, TextUtil textUtil) {
	   return getTermSize(textUtil.getTokens(surfaceForm).size());
   }


	public static Termsize getTermSize(int numberOfTokens) {

		switch (numberOfTokens) {
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


	public static Termsize getTermSize(SurfaceForm surfaceForm, TextUtil textUtil) {
		return getTermSize(surfaceForm.name(), textUtil);
	}

	@Override
	public String name() {
		return "Filter for the number of tokens in a term candidate.";
	}





}
