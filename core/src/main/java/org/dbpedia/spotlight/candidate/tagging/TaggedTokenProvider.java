package org.dbpedia.spotlight.candidate.tagging;

import java.util.List;


/**
 * A TaggedTokenProvider provides Part-of-Speech data for a range specified by two text offsets.
 * <p/>
 * General interface for providing TaggedToken objects. A TaggedToken contains the token,
 * the proposed part-of-speech tag, a confidence value and possible alternative tags.
 * <p/>
 * A TaggedTokenProvider will be initialized with a text and will return lists of
 * TaggedTokens for the words between two character positions in the text.
 *
 * @author jodaiber
 */
public interface TaggedTokenProvider {


	/**
	 * Returns a List of TaggedToken between the positions given as start and
	 * end text offsets.
	 *
	 * @param textOffsetStart text offset of the start (number of characters from the start of the text)
	 * @param textOffsetEnd   text offset of the end
	 * @return List of TaggedTokens
	 */
	public List<TaggedToken> getTaggedTokens(int textOffsetStart, int textOffsetEnd);


	/**
	 * Initialize the TaggedTokenProvider.
	 *
	 * @param text String that should be tagged.
	 */
	public void initialize(String text);


}
