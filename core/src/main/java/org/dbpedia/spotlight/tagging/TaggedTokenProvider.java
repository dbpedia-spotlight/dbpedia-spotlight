package org.dbpedia.spotlight.tagging;

import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;

import java.util.List;


/**
 * A TaggedTokenProvider provides part-of-speech data for a range specified by two text offsets.
 * <p/>
 * General interface for providing TaggedToken objects. A TaggedToken contains the token,
 * the proposed part-of-speech tag, a confidence value and possible alternative tags.
 * <p/>
 * A TaggedTokenProvider will be initialized with a text and will return lists of
 * TaggedTokens for the words between two character positions in the text.
 *
 * @author Joachim Daiber
 */
public interface TaggedTokenProvider {


	/**
	 * Returns a List of {@link TaggedToken}s between the positions given as start and
	 * end text offsets.
	 *
	 * @param textOffsetStart text offset for start of candidate term
	 * @param textOffsetEnd   text offset for end of candidate term
	 * @return List of TaggedTokens
	 */
	public List<TaggedToken> getTaggedTokens(int textOffsetStart, int textOffsetEnd);

	
	/**
	 * Returns a List of {@link TaggedToken}s between the positions given as start and
	 * end text offsets.
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return List of TaggedTokens
	 */
	public List<TaggedToken> getTaggedTokens(SurfaceFormOccurrence surfaceFormOccurrence);


	/**
	 * Returns the immediate left neighbour of the term candidate (defined by start
	 * and end text offset).
	 *
	 * @param textOffsetStart text offset for start of candidate term
	 * @param textOffsetEnd   text offset for end of candidate term
	 * @return TaggedToken immediately left of a candidate
	 * @throws org.dbpedia.spotlight.exceptions.ItemNotFoundException No left neighbour token found, first word in
	 *			the sentence?
	 */
	public TaggedToken getLeftNeighbourToken(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException;


	/**
	 * Returns the immediate left neighbour of the surface form occurrence.
	 *
	 * @see #getLeftNeighbourToken(int textOffsetStart, int textOffsetEnd)
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return TaggedToken immediately left of a candidate
	 * @throws 	org.dbpedia.spotlight.exceptions.ItemNotFoundException No left neighbour token found, first word in
	 *			the sentence?
	 */
	public TaggedToken getLeftNeighbourToken(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException;


	/**
	 * Returns the immediate right neighbour of the term candidate (defined by start
	 * and end text offset).
	 *
	 * @param textOffsetStart text offset for start of candidate term
	 * @param textOffsetEnd   text offset for end of candidate term
	 * @return TaggedToken immediately left of a candidate
	 * @throws 	org.dbpedia.spotlight.exceptions.ItemNotFoundException No right neighbour token found, last word in
	 *			the sentence?
	 */
	public TaggedToken getRightNeighbourToken(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException;


	/**
	 * Returns the immediate right neighbour of surface form occcurrence.
	 *
	 * @see #getRightNeighbourToken(int textOffsetStart, int textOffsetEnd)
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return TaggedToken immediately left of a candidate
	 * @throws 	org.dbpedia.spotlight.exceptions.ItemNotFoundException No right neighbour token found, last word in
	 *			the sentence?
	 */
	public TaggedToken getRightNeighbourToken(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException;


	/**
	 * Retrieve a number of {@link TaggedToken}s to the left of the surface form.
	 *
	 * @param surfaceFormOccurrence Surface form occurrence of the term candidate
	 * @param length the number of 
	 * @return List of {@link TaggedToken}s
	 * @throws ItemNotFoundException the tokens could not be retrieved
	 */
	public List<TaggedToken> getLeftContext(SurfaceFormOccurrence surfaceFormOccurrence, int length) throws ItemNotFoundException;


	/**
	 * Retrieve a number of {@link TaggedToken}s to the left of the surface form.
	 *
	 * @param textOffsetStart text offset for start of candidate
	 * @param textOffsetEnd   text offset for end of candidate
	 * @param length number of {@link TaggedToken}s to be retrieved
	 * @return List of {@link TaggedToken}s
	 * @throws ItemNotFoundException the tokens could not be retrieved
	 */
	public List<TaggedToken> getLeftContext(int textOffsetStart, int textOffsetEnd, int length) throws ItemNotFoundException;


	/**
	 * Retrieve a number of {@link TaggedToken}s to the right of the surface form.
	 *
	 * @param textOffsetStart text offset for start of candidate
	 * @param textOffsetEnd   text offset for end of candidate
	 * @param length number of {@link TaggedToken}s to be retrieved
	 * @return List of {@link TaggedToken}s
	 * @throws ItemNotFoundException the tokens could not be retrieved
	 */
	public List<TaggedToken> getRightContext(int textOffsetStart, int textOffsetEnd, int length) throws ItemNotFoundException;


	/**
	 * Retrieve a number of {@link TaggedToken}s to the right of surface form.
	 *
	 * @param surfaceFormOccurrence	the surface form occurrence
	 * @param length number of tokens to retrieve
	 * @return List of {@link TaggedToken}s
	 * @throws ItemNotFoundException the tokens could not be retrieved
	 */
	public List<TaggedToken> getRightContext(SurfaceFormOccurrence surfaceFormOccurrence, int length) throws ItemNotFoundException;


	/**
	 * Returns {@link TaggedToken}s for the sentence containing the term candidate (defined by start and
	 * end text offset).
	 *
	 * @param textOffsetStart text offset for start of candidate
	 * @param textOffsetEnd   text offset for end of candidate
	 * @return List of TaggedTokens in the sentence
	 * @throws org.dbpedia.spotlight.exceptions.ItemNotFoundException the sentence tokens cannot be retrieved
	 */
	public List<TaggedToken> getSentenceTokens(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException;


	/**
	 * Returns {@link TaggedToken}s for the sentence containing the term candidate (defined by start and
	 * end text offset).
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return List of TaggedTokens in the sentence
	 * @throws org.dbpedia.spotlight.exceptions.ItemNotFoundException the sentence tokens cannot be retrieved
	 */
	public List<TaggedToken> getSentenceTokens(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException;


	/**
	 * Get the entire sentence containing the mention identified by the two text offsets.
	 *
	 * @param textOffsetStart	text offset for start of candidate
	 * @param textOffsetEnd		text offset for end of candidate
	 * @return					the sentence containing the mention
	 * @throws ItemNotFoundException the sentence could not be retrieved
	 */
	public String getSentence(int textOffsetStart, int textOffsetEnd) throws ItemNotFoundException;


	/**
	 * Get the entire sentence containing the surface form occurrence.
	 *
	 * @param surfaceFormOccurrence the surface form occurrence
	 * @return the sentence containing the mention
	 * @throws ItemNotFoundException the sentence could not be retrieved
	 */
	public String getSentence(SurfaceFormOccurrence surfaceFormOccurrence) throws ItemNotFoundException;


	/**
	 * Is the candidate the first token in a sentence?
	 *
	 * @param textOffsetStart 	text offset for start of candidate
	 * @param textOffsetEnd   	text offset for end of candidate
	 * @return 					true iff the candidate is at the beginning of the sentence
	 */
	public boolean isSentenceInitial(int textOffsetStart, int textOffsetEnd);


	/**
	 * Is the candidate the first token in a sentence?
	 *
	 * @param surfaceFormOccurrence surface form occurrence of the term candidate
	 * @return true iff the term candidate is at the beginning of the sentence
	 */
	public boolean isSentenceInitial(SurfaceFormOccurrence surfaceFormOccurrence);


	/**
	 * Return all possible unigram candidates (all nouns/fw) in the text.
	 *
	 * @return list of all unigram nouns that are possible candidates
	 */
	public List<SurfaceFormOccurrence> getUnigramCandidates();

	
	/**
	 * Initialize the TaggedTokenProvider.
	 *
	 * @param text String that should be tagged.
	 */
	public void initialize(String text);

}
